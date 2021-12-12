#include <liburing.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <linux/stat.h>
#include "include/org_reactivetoolbox_io_uring_Uring.h"

#define RING_PTR        ((struct io_uring *) base_address)
#define CQE_BATCH_PTR   ((struct io_uring_cqe **) completions_address)
#define COUNT           ((unsigned) count)

JNIEXPORT jint JNICALL Java_org_reactivetoolbox_io_uring_Uring_init(JNIEnv *env, jclass clazz, jint num_entries, jlong base_address, jint flags) {
    return (jint) io_uring_queue_init((unsigned) num_entries, RING_PTR, (unsigned) flags);
}

JNIEXPORT void JNICALL Java_org_reactivetoolbox_io_uring_Uring_close(JNIEnv *env, jclass clazz, jlong base_address) {
    io_uring_queue_exit(RING_PTR);
}

JNIEXPORT jint JNICALL Java_org_reactivetoolbox_io_uring_Uring_peekCQ(JNIEnv *env, jclass clazz, jlong base_address, jlong completions_address, jlong count) {
    return (jint) io_uring_peek_batch_cqe(RING_PTR, CQE_BATCH_PTR, COUNT);
}

JNIEXPORT void JNICALL Java_org_reactivetoolbox_io_uring_Uring_advanceCQ(JNIEnv *env, jclass clazz, jlong base_address, jlong count) {
    io_uring_cq_advance(RING_PTR, COUNT);
}

JNIEXPORT jint JNICALL Java_org_reactivetoolbox_io_uring_Uring_readyCQ(JNIEnv *env, jclass clazz, jlong base_address) {
    return (jint) io_uring_cq_ready(RING_PTR);
}

JNIEXPORT jlong JNICALL Java_org_reactivetoolbox_io_uring_Uring_spaceLeft(JNIEnv *env, jclass clazz, jlong base_address) {
    return (jlong) io_uring_sq_space_left(RING_PTR);
}

JNIEXPORT jlong JNICALL Java_org_reactivetoolbox_io_uring_Uring_nextSQEntry(JNIEnv *env, jclass clazz, jlong base_address) {
    if (io_uring_sq_space_left(RING_PTR) < 1) {
        io_uring_submit_and_wait(RING_PTR, 1);
    }

    return (jlong) io_uring_get_sqe(RING_PTR);
}

JNIEXPORT jint JNICALL Java_org_reactivetoolbox_io_uring_Uring_peekSQEntries(JNIEnv *env, jclass clazz, jlong base_address, jlong submissions_address, jlong space) {
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

    return (jint) count;
}

JNIEXPORT jlong JNICALL Java_org_reactivetoolbox_io_uring_Uring_submitAndWait(JNIEnv *env, jclass clazz, jlong base_address, jint count) {
    return (jlong) io_uring_submit_and_wait(RING_PTR, COUNT);
}

//-----------------------------------------------------
// Socket API
//-----------------------------------------------------
#define INT_SO_KEEPALIVE 0x0001
#define INT_SO_REUSEADDR 0x0002
#define INT_SO_REUSEPORT 0x0004
#define INT_SO_LINGER    0x0008

static jint get_errno(void) {
    int rc = errno;
    return rc > 0 ? -rc : rc;
}

static int set_binary_option(int sock, int option) {
    const int val = 1;
    return setsockopt(sock, SOL_SOCKET, option, &val, sizeof(val));
}

JNIEXPORT jint JNICALL Java_org_reactivetoolbox_io_uring_Uring_socket(JNIEnv *env, jclass clazz, jint domain, jint socket_type, jint socket_options) {
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

    return (jint) sock;
}

JNIEXPORT jint JNICALL Java_org_reactivetoolbox_io_uring_Uring_prepareForListen(JNIEnv *env, jclass clazz, jint sock, jlong address, jint len, jint queue_depth) {
    if(bind((int) sock, (struct sockaddr *)address, len)) {
        return get_errno();
    }

    if (listen((int) sock, (int) queue_depth)) {
        return get_errno();
    }

    return 0;
}
