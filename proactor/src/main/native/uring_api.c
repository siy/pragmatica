#include <liburing.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <asm-generic/socket.h>
#include <netinet/in.h>
#include <linux/stat.h>
#include <jni.h>

#define RING_PTR        ((struct io_uring *) base_address)
#define PARAMS_PTR        ((struct io_uring_params *) (base_address + 256))

int __sys_io_uring_enter(int fd, unsigned to_submit, unsigned min_complete, unsigned flags, sigset_t *sig);
int __sys_io_uring_register(int fd, unsigned opcode, const void *arg, unsigned nr_args);

JNIEXPORT jint JNICALL Java_org_pragmatica_io_async_uring_UringApi_init(JNIEnv *env, jclass clazz, jint num_entries, jlong base_address) {
    jint rc = (jint) io_uring_queue_init_params((unsigned) num_entries, RING_PTR, PARAMS_PTR);

    if (rc == 0) {
        io_uring_ring_dontfork(RING_PTR);
    }

    return rc;
}

JNIEXPORT void JNICALL Java_org_pragmatica_io_async_uring_UringApi_close(JNIEnv *env, jclass clazz, jlong base_address) {
    io_uring_queue_exit(RING_PTR);
}

JNIEXPORT jint JNICALL Java_org_pragmatica_io_async_uring_UringApi_enter(JNIEnv *env, jclass clazz, jint fd, jlong to_submit, jlong min_complete, jint flags) {
    return (jint) __sys_io_uring_enter(fd, (unsigned) to_submit, (unsigned) min_complete, (unsigned) flags, NULL);
}

JNIEXPORT jint JNICALL Java_org_pragmatica_io_async_uring_UringApi_register(JNIEnv *env, jclass clazz, jint fd, jint opcode, jlong arg, jlong nr_args) {
    int rc = __sys_io_uring_register(fd, (unsigned) opcode, (const void*) arg, (unsigned) nr_args);

    return (rc < 0) ? -errno : rc;
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

JNIEXPORT jint JNICALL Java_org_pragmatica_io_async_uring_UringApi_socket(JNIEnv *env, jclass clazz, jint domain, jint socket_type, jint socket_options) {
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

JNIEXPORT jint JNICALL Java_org_pragmatica_io_async_uring_UringApi_prepareForListen(JNIEnv *env, jclass clazz, jint sock, jlong address, jint len, jint queue_depth) {
    if(bind((int) sock, (struct sockaddr *)address, len)) {
        return get_errno();
    }

    if (listen((int) sock, (int) queue_depth)) {
        return get_errno();
    }

    return 0;
}
