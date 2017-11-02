FROM centos:7

ENV JAVA_HOME="/opt/java"
ENV PATH='${JAVA_HOME}/bin:$PATH'

RUN yum update -y && \
    yum install -y epel-release && \
    yum update -y && \
    yum install -y curl git htop vim xauth xclock xsel wget && \
    wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
       http://download.oracle.com/otn-pub/java/jdk/8u152-b16/aa0333dd3019491ca4f6ddbe78cdb6d0/jdk-8u152-linux-x64.tar.gz && \
    sha256sum jdk-8u152-linux-x64.tar.gz | awk '$1=="218b3b340c3f6d05d940b817d0270dfe0cfd657a636bad074dcabe0c111961bf"{print "Valid Oracle JDK checksum"}' && \
    tar xzvf jdk-8u152-linux-x64.tar.gz -C /opt && \
    ln -s /opt/jdk1.8.0_152 /opt/java && \
    rm -f jdk-8u152-linux-x64.tar.gz && \
    { \
       echo 'export JAVA_HOME=/opt/java' ; \
       echo 'export PATH=$PATH:$JAVA_HOME/bin' ; \
    } >> /etc/profile.d/java.sh && \
    wget -qO- http://apache.mediamirrors.org/maven/maven-3/3.5.2/binaries/apache-maven-3.5.2-bin.tar.gz | tar xzvf - -C /opt/ && \
    ln -s /opt/apache-maven-3.5.2 /opt/maven && \
    { \
       echo 'export MAVEN_HOME=/opt/maven' ; \
       echo 'export PATH=$PATH:$MAVEN_HOME/bin' ; \
    } >> /etc/profile.d/maven.sh && \
    yum clean all && \
    rm -rf /tmp/* && \
    rm -rf /var/cache/yum/* && \
    rm -rf /root/.cache 

EXPOSE 8080
EXPOSE 8000
CMD ["/bin/bash"]