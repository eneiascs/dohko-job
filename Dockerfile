FROM centos:7

ENV JAVA_HOME="/opt/java"
# ENV PATH='${JAVA_HOME}/bin:$PATH'

RUN yum update -y && \
    yum install -y epel-release && \
    yum update -y && \
    yum install -y curl bc git htop sysstat vim xauth xclock xsel wget && \
    wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
        http://download.oracle.com/otn-pub/java/jdk/8u162-b12/0da788060d494f5095bf8624735fa2f1/jdk-8u162-linux-x64.tar.gz && \
    sha256sum jdk-8u162-linux-x64.tar.gz | awk '$1=="68ec82d47fd9c2b8eb84225b6db398a72008285fafc98631b1ff8d2229680257"{print "Valid Oracle JDK checksum"}' && \
    tar xzvf jdk-8u162-linux-x64.tar.gz -C /opt && \
    ln -s /opt/jdk1.8.0_162 /opt/java && \
    rm -f jdk-8u162-linux-x64.tar.gz && \
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
    yum install -y time &&\
    curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py" &&\
    python get-pip.py &&\
    pip install benchexec &&\		
    yum clean all && \
    rm -rf /tmp/* && \
    rm -rf /var/cache/yum/* && \
    rm -rf /root/.cache 
COPY run /opt/dohko/job/run
COPY target /opt/dohko/job/target
EXPOSE 8080
EXPOSE 8000
CMD ["/bin/bash"]
