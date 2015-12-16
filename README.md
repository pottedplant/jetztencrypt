# Disclaimer

FOR EDUCATIONAL PURPOSED ONLY. NO WARRANTY.

# jetztencrypt

Simple letsencrypt (https://letsencrypt.org/) client.

## Download

* [jetztencrypt-v0.2-rc1-app.jar](https://jitpack.io/com/github/pottedplant/jetztencrypt/v0.2-rc1/jetztencrypt-v0.2-rc1-app.jar)

## Usage

```
java -jar jetztencrypt-v0.2-rc1-app.jar -- help
```

### Quick and Dirty (privileged user)

```
java -jar jetztencrypt-v0.2-rc1-app.jar -- \
  --account-key account.key \
  --certificate-key certificate.key \
  --certificate certificate.crt \
  --mode server \
  --server-bind-port 80 \
  --server-bind-address 0.0.0.0 \
  --embedded-identrust-root \
  --hostname some.domain \
  --alt-name some.domain \
  --alt-name www.some.domain
```

## Build

```
./gradlew bundle
```

The bundled application jar can be found at ```build/libs/jetztencrypt-*-app.jar```.
