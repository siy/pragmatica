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
#define CQE_ARRAY_PTR   ((struct io_uring_cqe *) completions_address)
#define COUNT           ((unsigned) count)

#define SQ_ENTRY_SIZE       48
#define SUBMIT_IMMEDIATE    0x01
#define SUBMIT_WAIT         0x02

/* Initialize the ring */
int ring_open(int num_entries, long base_address, int flags) {
    return io_uring_queue_init((unsigned) num_entries, RING_PTR, (unsigned) flags);
}

/* Shutdown the ring */
void ring_close(long base_address) {
    io_uring_queue_exit(RING_PTR);
}

int ring_copy_cqes(long base_address, long completions_address, int count) {
    struct io_uring *ring = RING_PTR;
    struct io_uring_cqe *cqe = CQE_ARRAY_PTR;

    unsigned ready = io_uring_cq_ready(ring);

    if (!ready) {
        return 0;
    }

    unsigned head = *ring->cq.khead;
    unsigned mask = ring->cq.ring_mask;

    count = count > ready ? ready : count;

    unsigned last = head + count;

    for (;head != last; head++) {
        *cqe++ = ring->cq.cqes[head & mask];
    }

    io_uring_cq_advance(ring, count);

    return count;
}

int ring_direct_submit(long base_address, long submission_entries, int count, int flags) {
    unsigned char* filled_entries = (unsigned char*) submission_entries;

    for (int i = 0; i < count; i++, filled_entries += SQ_ENTRY_SIZE) {
        struct io_uring_sqe* entry = io_uring_get_sqe(RING_PTR);

        memcpy(entry, filled_entries, SQ_ENTRY_SIZE);
    }

    if (flags & SUBMIT_IMMEDIATE) {
        int wait_cnt = (flags & SUBMIT_WAIT) ? count : 0;

        return io_uring_submit_and_wait(RING_PTR, wait_cnt);
    }

    return 0;
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
