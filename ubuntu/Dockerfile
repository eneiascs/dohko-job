FROM ubuntu:16.04

ENV JAVA_HOME="/opt/java"
# ENV PATH='${JAVA_HOME}/bin:$PATH'

RUN apt-get update -y && \
    apt-get install -y wget vim time python2.7 python-pip && \
    wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
       http://download.oracle.com/otn-pub/java/jdk/8u172-b11/a58eab1ec242421181065cdc37240b08/jdk-8u172-linux-x64.tar.gz && \
    sha256sum jdk-8u172-linux-x64.tar.gz | awk '$1=="28a00b9400b6913563553e09e8024c286b506d8523334c93ddec6c9ec7e9d346"{print "Valid Oracle JDK checksum"}' && \
    tar xzvf jdk-8u172-linux-x64.tar.gz -C /opt && \
    ln -s /opt/jdk1.8.0_172 /opt/java && \
    rm -f jdk-8u172-linux-x64.tar.gz && \
    { \
       echo 'export JAVA_HOME=/opt/java' ; \
       echo 'export PATH=$PATH:$JAVA_HOME/bin' ; \
    } >> ~/.bashrc && \
#    pip install --upgrade pip && \	
    pip install benchexec &&\		
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/* && \	
    rm -rf /root/.cache 
COPY run /opt/dohko/job/run
COPY target /opt/dohko/job/target
EXPOSE 8080
EXPOSE 8000
CMD ["/bin/bash"]
