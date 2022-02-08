cd src/main/java
javac --enable-preview --release 17 -h ../native/include/ -d ../../../target/classes org/pfj/io/async/uring/UringNative.java
