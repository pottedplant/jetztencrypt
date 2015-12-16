# Disclaimer

FOR EDUCATIONAL PURPOSED ONLY. NO WARRANTY.

# jetztencrypt

Simple letsencrypt (https://letsencrypt.org/) client.

## Download

* [jetztencrypt-v0.3-app.jar](https://jitpack.io/com/github/pottedplant/jetztencrypt/v0.3/jetztencrypt-v0.3-app.jar)

> Dependencies: **Java 8**

## Usage

```
java -jar jetztencrypt-v0.3-app.jar --help
```

### Quick and Dirty (privileged user)

```
java -jar jetztencrypt-v0.3-app.jar \
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

## Tips

When installing a cron job use ```--log-level warn``` to silence the output.

You may want to use a wrapper script for easier crontab maintenance:
```bash
#!/usr/bin/env bash
java -jar /path/to/jetztencrypt-app.jar \
  --account-key /path/to/letsencrypt.key \
  --certificate-key /path/to/server.key \
  --certificate /path/to/server.crt \
  --mode directory \
  --acme-directory /path/to/acme/ \
  --embedded-identrust-root \
  --log-level warn \
  --hostname some.domain \
  --alt-name www.some.domain --alt-name www2.some.domain \
&& ( /etc/init.d/nginx reload > /dev/null )
```

Vanilla JDK/JRE installations might not have the required IdenTrust CA certificate installed. Until this changes you may use  ```--embedded-identrust-root``` to use a bundeled IdenTrust certificate to prevent *letsencrypt* api calls from failing.

## Build

```
./gradlew bundle
```

The bundled application jar can be found at ```build/libs/jetztencrypt-*-app.jar```.
