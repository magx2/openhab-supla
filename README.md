# Supla ❤️ Open HAB

## FAQ

### SSL Problem

In some Java environments, unlimited cryptography might be turned off. To enable it, you need to set the `crypto.policy` to `unlimited`.

For more information, you can read the post [Enabling Unlimited Strength Cryptography in Java](https://www.baeldung.com/jce-enable-unlimited-strength).

**Note:** In a Docker environment, it is sufficient to set the environment variable `CRYPTO_POLICY` to `unlimited`.


### Disabled algorithms

This chapter provides instructions on how to enable TLS 1.0 and TLS 1.1, which are disabled by default starting from Java 8.

#### Enabling TLS 1.0 and TLS 1.1

Follow the steps below to enable TLS 1.0 and TLS 1.1:

1. Navigate to the `java.security` file located in your Java installation directory. The file can be found in either of the following paths:
	- `$JAVA_HOME/conf/security`
	- `$JAVA_HOME/lib/security`

2. Open the `java.security` file and locate the `jdk.tls.disabledAlgorithms` property.

3. Remove `SSLv3`, `TLSv1`, and `TLSv1.1` from the `jdk.tls.disabledAlgorithms` property.

4. Save and close the `java.security` file.

##### Docker

1. Log in into container: `docker exec -it <docker-container-id> /bin/bash`
2. Go to folder with `java.security`: `cd /usr/lib/jvm/java-17-openjdk-amd64/conf/security`
3. Do a backup: `cp java.security java.security.bak`
4. Check current settings: `cat java.security | grep jdk.tls.disabledAlgorithms`
5. Replace it: `sed -i 's/jdk.tls.disabledAlgorithms=SSLv3, TLSv1, TLSv1.1, RC4, DES, MD5withRSA, \\/jdk.tls.disabledAlgorithms=RC4, DES, MD5withRSA, \\/g' java.security`
6. Check changed settings: `cat java.security | grep jdk.tls.disabledAlgorithms`
7. Exit and restart docker container

#### Restarting OpenHAB

After enabling TLS 1.0 and TLS 1.1, a restart of OpenHAB is required for the changes to take effect. Please follow the standard procedure for restarting your OpenHAB instance.

**Note:** Always ensure to backup any files or settings before making changes. This will help you to restore the original settings if something goes wrong.

## Support ❤️

If you want to support the author of this binding, buy him a coffee:

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/S6S8UBWWY) <a href="https://buycoffee.to/magx2" target="_blank"><img src="https://buycoffee.to/btn/buycoffeeto-btn-primary.svg" alt="Postaw mi kawę na buycoffee.to" width="150"></a>
