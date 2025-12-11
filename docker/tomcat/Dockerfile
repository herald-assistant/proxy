FROM tomcat:11.0-jdk21-temurin

# Użytkownik admin/admin
COPY tomcat-users.xml /usr/local/tomcat/conf/tomcat-users.xml

# Własne context.xml dla managera (bez RemoteAddrValve)
COPY manager-context.xml /usr/local/tomcat/manager-context.xml

RUN set -eux; \
    cd /usr/local/tomcat; \
    # Włącz standardowe aplikacje
    cp -av webapps.dist/ROOT    webapps/ROOT; \
    cp -av webapps.dist/manager webapps/manager; \
    # Podmień context.xml managera na nasz
    cp -v manager-context.xml webapps/manager/META-INF/context.xml


