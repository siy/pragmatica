#include <liburing.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <asm-generic/socket.h>
#include <netinet/in.h>
#include <linux/stat.h>

#define RING_PTR        ((struct io_uring *) base_address)
#define CQE_BATCH_PTR   ((struct io_uring_cqe **) completions_address)
#define COUNT           ((unsigned) count)

int native_init(int num_entries, long base_address, int flags) {
    return io_uring_queue_init((unsigned) num_entries, RING_PTR, (unsigned) flags);
}

void native_close(long base_address) {
    io_uring_queue_exit(RING_PTR);
}

int native_peekCQ(long base_address, long completions_address, long count) {
    return io_uring_peek_batch_cqe(RING_PTR, CQE_BATCH_PTR, COUNT);
}

void native_advanceCQ(long base_address, long count) {
    io_uring_cq_advance(RING_PTR, COUNT);
}

long native_nextSQEntry(long base_address) {
    if (io_uring_sq_space_left(RING_PTR) < 1) {
        io_uring_submit_and_wait(RING_PTR, 1);
    }

    return (long) io_uring_get_sqe(RING_PTR);
}

int native_peekSQEntries(long base_address, long submissions_address, long space) {
    if (io_uring_sq_space_left(RING_PTR) < 1) {
        io_uring_submit_and_wait(RING_PTR, 1);
    }

    int count = 0;
    struct io_uring_sqe** buffer = (struct io_uring_sqe **) submissions_address;

    for(count = 0; count < space; count++) {
        struct io_uring_sqe* entry = io_uring_get_sqe(RING_PTR);

        if (!entry) {
            break;
        }

        buffer[count] = entry;
    }

    return count;
}

long native_submitAndWait(long base_address, int count) {
    return io_uring_submit_and_wait(RING_PTR, COUNT);
}

//-----------------------------------------------------
// Socket API
//-----------------------------------------------------
#define INT_SO_KEEPALIVE 0x0001
#define INT_SO_REUSEADDR 0x0002
#define INT_SO_REUSEPORT 0x0004
#define INT_SO_LINGER    0x0008

static int get_errno(void) {
    int rc = errno;
    return rc > 0 ? -rc : rc;
}

static int set_binary_option(int sock, int option) {
    const int val = 1;
    return setsockopt(sock, SOL_SOCKET, option, &val, sizeof(val));
}

int native_socket(int domain, int socket_type, int socket_options) {
    int sock = socket((int) domain, (int) socket_type, 0);

    if (sock < 0) {
        return get_errno();
    }

    if ((socket_options & INT_SO_KEEPALIVE) && set_binary_option(sock, SO_KEEPALIVE)) {
        return get_errno();
    }

    if ((socket_options & INT_SO_REUSEADDR) && set_binary_option(sock, SO_REUSEADDR)) {
        return get_errno();
    }

    if ((socket_options & INT_SO_REUSEPORT) && set_binary_option(sock, SO_REUSEPORT)) {
        return get_errno();
    }

    if (socket_options & INT_SO_LINGER) {
        struct linger so_linger;
        so_linger.l_onoff = 1;
        so_linger.l_linger = 0;

        if(setsockopt(sock, SOL_SOCKET, SO_LINGER, &so_linger, sizeof(so_linger))) {
            return get_errno();
        }
    }

    return sock;
}

int native_prepareForListen(int sock, long address, int len, int queue_depth) {
    if(bind((int) sock, (struct sockaddr *)address, len)) {
        return get_errno();
    }

    if (listen((int) sock, (int) queue_depth)) {
        return get_errno();
    }

    return 0;
}
