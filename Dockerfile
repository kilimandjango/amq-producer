FROM openjdk8:latest

MAINTAINER Kilian Henneboehle kilian.henneboehle@gmx.de

LABEL openshift.io.tags="amq-producer-builder-image" \
      io.openshift.s2i.scripts-url=image:///usr/libexec/s2i

ADD ./s2i/bin /usr/libexec/s2i

COPY ./contrib/amq-s2i /usr/local/bin

RUN yum-config-manager --disable epel >/dev/null || : && \
    yum-config-manager --enable rhel-7-server-ose-3.6-rpms || : && \
    yum -y update && \
    yum clean all && \
    chmod a+x /usr/local/bin/fix-permissions && \ 
    /usr/local/bin/fix-permissions /usr/libexec/s2i 
    
RUN echo $PATH
    
USER 1001

CMD ["/usr/libexec/s2i/run"]
