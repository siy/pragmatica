#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include "liburing.h"

int main(int argc, char** argv) {
    struct io_uring_params params;

    printf("public interface IoUringParamsOffsets {\n");
    printf("    int SIZE=%ld;\n", sizeof(struct io_uring_params));
    printf("    RawProperty %s = RawProperty.raw(%ld, %ld);\n", "sq_entries", offsetof(struct io_uring_params, sq_entries), sizeof(params.sq_entries));
    printf("    RawProperty %s = RawProperty.raw(%ld, %ld);\n", "cq_entries", offsetof(struct io_uring_params, cq_entries), sizeof(params.cq_entries));
    printf("    RawProperty %s = RawProperty.raw(%ld, %ld);\n", "flags", offsetof(struct io_uring_params, flags), sizeof(params.flags));
    printf("    RawProperty %s = RawProperty.raw(%ld, %ld);\n", "sq_thread_cpu", offsetof(struct io_uring_params, sq_thread_cpu), sizeof(params.sq_thread_cpu));
    printf("    RawProperty %s = RawProperty.raw(%ld, %ld);\n", "sq_thread_idle", offsetof(struct io_uring_params, sq_thread_idle), sizeof(params.sq_thread_idle));
    printf("    RawProperty %s = RawProperty.raw(%ld, %ld);\n", "features", offsetof(struct io_uring_params, features), sizeof(params.features));
    printf("    RawProperty %s = RawProperty.raw(%ld, %ld);\n", "wq_fd", offsetof(struct io_uring_params, wq_fd), sizeof(params.wq_fd));
    printf("    RawProperty %s = RawProperty.raw(%ld, %ld);\n", "sq_off", offsetof(struct io_uring_params, sq_off), sizeof(params.sq_off));
    printf("    RawProperty %s = RawProperty.raw(%ld, %ld);\n", "cq_off", offsetof(struct io_uring_params, cq_off), sizeof(params.cq_off));
    printf("}\n");
    printf("io_uring size: %ld\n", sizeof(struct io_uring));
}
