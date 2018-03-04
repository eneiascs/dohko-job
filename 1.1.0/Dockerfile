FROM eneiascs/dohko-job:1.0.0
ENV JAVA_HOME="/opt/java"
COPY target /opt/dohko/job/target
COPY removeTemp.sh /opt/dohko/removeTemp.sh
EXPOSE 8080
EXPOSE 8000
CMD ["/bin/bash"]
