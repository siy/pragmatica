#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <asm-generic/socket.h>
#include <netinet/in.h>
#include <linux/stat.h>

#include <liburing.h>
#include <syscall.h>


#define RING_PTR        ((struct io_uring *) base_address)
#define CQE_BATCH_PTR   ((struct io_uring_cqe **) completions_address)
#define COUNT           ((unsigned) count)

/* Initialize the ring */
int ring_init(int num_entries, long base_address, int flags) {
    return io_uring_queue_init((unsigned) num_entries, RING_PTR, (unsigned) flags);
//
//    if (rc == 0) {
//        io_uring_dontfork(RING_PTR);
//    }
//
//    return rc;
}

/* Shutdown the ring */
void ring_exit(long base_address) {
    io_uring_queue_exit(RING_PTR);
}

/* Retrieve batch of ready completions */
int ring_peek_batch_cqe(long base_address, long completions_address, long count) {
    return io_uring_peek_batch_cqe(RING_PTR, CQE_BATCH_PTR, COUNT);
}

/* Advance completion queue */
void ring_cq_advance(long base_address, long count) {
    io_uring_cq_advance(RING_PTR, COUNT);
}

/* Retrieve batch of ready completions and advance completion queue */
int ring_peek_batch_and_advance_cqe(long base_address, long completions_address, long count) {
    int rc = io_uring_peek_batch_cqe(RING_PTR, CQE_BATCH_PTR, COUNT);

    if (rc > 0) {
        io_uring_cq_advance(RING_PTR, rc);
    }

    return rc;
}

/* Get next available submission entry (0L if queue full) */
long ring_get_sqe(long base_address) {
    return (long) io_uring_get_sqe(RING_PTR);
}

/* Get array of available submissions entries */
int ring_peek_batch_sqe(long base_address, long submissions_address, long space) {
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

/* Submit filled entries and wait for at lest specified number of available events */
long ring_submit_and_wait(long base_address, int count) {
    return io_uring_submit_and_wait(RING_PTR, COUNT);
}

/* Perform register operation */
int ring_register(long base_address, int opcode, long arg, long nr_args) {
    int rc = __sys_io_uring_register(RING_PTR->ring_fd, (unsigned) opcode, (const void*) arg, (unsigned) nr_args);

    return (rc < 0) ? -errno : rc;
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

/* Open socket */
int ring_socket(int domain, int socket_type, int socket_options) {
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

/* Configure socket for listen operation */
int ring_listen(int sock, long address, int len, int queue_depth) {
    if(bind((int) sock, (struct sockaddr *)address, len)) {
        return get_errno();
    }

    if (listen((int) sock, (int) queue_depth)) {
        return get_errno();
    }

    return 0;
}
