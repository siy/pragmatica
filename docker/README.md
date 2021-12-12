# IO_URING Builder

### Build Image
```sh
docker build -t pragmatica-builder:latest .

```
In some cases command may require `sudo` to obtain necessary privileges.

### Use Image

```sh
docker run --rm -ti -v "$(realpath .):/app" pragmatica-builder:latest
```
The command above opens a shell with project directory linked to `/app` directory inside image. 
This enables convenient building of the binaries and transparent sharing of the files between project directory on the host
machine and `/app` directory inside the image.
