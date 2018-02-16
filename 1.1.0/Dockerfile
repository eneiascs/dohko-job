FROM eneiascs/dohko-job:1.0.0

RUN chkconfig crond on
RUN touch /var/log/cron.log
RUN (crontab -l ; echo "*/5 * * * * /opt/dohko/job/removeTemp.sh >> /var/log/cron.log") | crontab
CMD ["/bin/bash"]
