# Disclaimer

FOR EDUCATIONAL PURPOSED ONLY. NO WARRANTY.

# jetztencrypt

Simple letsencrypt (https://letsencrypt.org/) client.

## Download

* [jetztencrypt-v0.2-rc3-app.jar](https://jitpack.io/com/github/pottedplant/jetztencrypt/v0.2-rc3/jetztencrypt-v0.2-rc3-app.jar)

> Dependencies: **Java 8**

## Usage

```
java -jar jetztencrypt-v0.2-rc3-app.jar --help
```

### Quick and Dirty (privileged user)

```
java -jar jetztencrypt-v0.2-rc3-app.jar \
  --account-key account.key \
  --certificate-key certificate.key \
  --certificate certificate.crt \
  --mode server \
  --server-bind-port 80 \
  --server-bind-address 0.0.0.0 \
  --embedded-identrust-root \
  --hostname some.domain --alt-name some.domain --alt-name www.some.domain
```

*jetztencrypt* will check if a suitable certificate (with some days left before it expires) is present or create a new one otherwise. If no ```account.key``` file is present a new one will be generated.

The PEM encoded private key file ```certificate.key``` and certificate chain ```certificate.crt``` may be directly referenced from *nginx*.

## nginx: passthrough

```nginx
upstream jetztencrypt {
  server localhost:8080;
}

server {
  listen 80;
  
  location /.well-known/acme-challenge/ {
    proxy_pass http://jetztencrypt;
  }
}

```

## nginx: directory mode

```nginx
server {
  listen 80;
  
  location /.well-known/acme-challenge/ {
    alias /path/to/acme/dir/;
  }
}
```

Instead of ```--mode server``` use ```--mode directory --acme-directory /path/to/acme/dir/```.

## Build

```
./gradlew bundle
```

The bundled application jar can be found at ```build/libs/jetztencrypt-*-app.jar```.
