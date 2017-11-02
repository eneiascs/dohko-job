# -*- mode: ruby -*-
# vi: set ft=ruby :

ENV["LC_ALL"] = "en_US.UTF-8"
Vagrant.require_version ">= 1.9.2"

class FixGuestAdditions < VagrantVbguest::Installers::RedHat
    def dependencies
        packages = super

        # If there's no "kernel-devel" package matching the running kernel in the
        # default repositories, then the base box we're using doesn't match the
        # latest CentOS release anymore and we have to look for it in the archives...
        if communicate.test('test -f /etc/centos-release && ! yum -q info kernel-devel-`uname -r` &>/dev/null')
            env.ui.warn("[#{vm.name}] Looking for the CentOS 'kernel-devel' package in the release archives...")
            packages.sub!('kernel-devel-`uname -r`', 'http://mirror.centos.org/centos' \
                                                     '/`grep -Po \'\b\d+\.[\d.]+\b\' /etc/centos-release`' \
                                                     '/{os,updates}/`arch`/Packages/kernel-devel-`uname -r`.rpm')
        end

        packages
    end
end

def install_plugins (plugins=[], restart=false)
  installed = false

  plugins.each do |plugin|
    unless Vagrant.has_plugin? plugin 
      system ("vagrant plugin install #{plugin}")
      puts "Plugin #{plugin} installed!"
      installed = true
    end
  end

  if installed and restart
    puts "Dependencies installed, restarting vagrant ..."
    exec "vagrant #{ARGV.join(' ')}"
  end
  
  return installed
end

plugins = ["vagrant-docker-compose",
  "vagrant-vbguest",
  "vagrant-proxyconf",
  "vagrant-proxyconf",
  "vagrant-env",
  "vagrant-persistent-storage",
  "vagrant-cachier"]

install_plugins(plugins, restart = true)

def disable_ipv4_forwarding(node)
  node.vm.provision "shell", inline: <<-SHELL

    function systctl_set() {
      key=$1
      value=$2
      config_path='/etc/sysctl.conf'
      # set now
      sysctl -w ${key}=${value}
      # persist on reboot
      if grep -q "${key}" "${config_path}"; then
        sed -i "s/^${key}.*$/${key} = ${value}/" "${config_path}"
      else
        echo "${key} = ${value}" >> "${config_path}"
      fi
    }

    echo '>>> Enabling IPv4 Forwarding'
    systctl_set net.ipv4.ip_forward 1
  SHELL
end

Vagrant.configure("2") do |config|
  config.vm.box = "centos-7-x86_64-vagrant-1709_01"
  config.vm.box_url = "http://cloud.centos.org/centos/7/vagrant/x86_64/images/CentOS-7-x86_64-Vagrant-1709_01.VirtualBox.box"
  config.vm.box_download_checksum = "ed1dd5a19235d42e7d55729471fc3479c9d0e385042e425f69d15df4b0e09e85"
  config.vm.box_download_checksum_type = "sha256"
  
  if Vagrant.has_plugin?('vagrant-vbguest')
    config.vbguest.installer = FixGuestAdditions
  end

  if Vagrant.has_plugin?("vagrant-cachier")
      config.cache.scope       = :box
      config.cache.auto_detect = true
  end

  config.ssh.forward_agent = true
  config.ssh.keys_only = true
  config.ssh.forward_x11 = true

  config.vm.network "private_network", ip: "10.10.3.12"
  config.vm.network "forwarded_port", guest: 3306, host: 3306

  config.vm.synced_folder ".", "/vagrant", disable: true
  config.vm.synced_folder ".", "/home/vagrant/workspace", disable: false

  disable_ipv4_forwarding(config)

  config.vm.provision "shell", inline: <<-SHELL
     yum update -y
     yum install -y epel-release
     yum update -y 
     yum groupinstall -y 'Development Tools'
     yum install -y curl git htop vim xauth xclock xsel wget

     # Install Oracle Java 8
     wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
     http://download.oracle.com/otn-pub/java/jdk/8u152-b16/aa0333dd3019491ca4f6ddbe78cdb6d0/jdk-8u152-linux-x64.tar.gz

     sha256sum jdk-8u152-linux-x64.tar.gz | awk '$1=="218b3b340c3f6d05d940b817d0270dfe0cfd657a636bad074dcabe0c111961bf"{print "Error on downloading Oracle JDK. Invalid checksum"; exit}'
     tar xzvf jdk-8u152-linux-x64.tar.gz -C /opt
     ln -s /opt/jdk1.8.0_152 /opt/java
     rm -f jdk-8u152-linux-x64.tar.gz

     { \
       echo 'export JAVA_HOME=/opt/java' ; \
       echo 'export PATH=$PATH:$JAVA_HOME/bin' ; \
     } >> /etc/profile.d/java.sh

     chown -R vagrant:vagrant /opt/java

     # yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel

     wget -qO- http://apache.mediamirrors.org/maven/maven-3/3.5.2/binaries/apache-maven-3.5.2-bin.tar.gz | tar xzvf - -C /opt/
     ln -s /opt/apache-maven-3.5.2 /opt/maven
     chown -R vagrant:vagrant /opt/maven

     # rm -rf /etc/maven

     { \
       echo 'export MAVEN_HOME=/opt/maven' ; \
       echo 'export PATH=$PATH:$MAVEN_HOME/bin' ; \
     } >> /etc/profile.d/maven.sh

     { 
       echo 'export MYSQL_CONTAINER_NAME=mysql' ; \
       echo 'alias mysql="docker exec -it ${MYSQL_CONTAINER_NAME} mysql "$@""' ; \
       echo ; \
     } >> /etc/.aliases

     echo 'source /etc/.aliases' >> /etc/bashrc

  SHELL

  config.vm.provider "virtualbox" do |vb|
    vb.customize ["modifyvm", :id, "--memory", 2048]
    vb.customize ["modifyvm", :id, "--ioapic", "on", "--cpus", 4]
    vb.name = "dohkojob"
  end

  config.vm.provision :docker
  config.vm.provision :docker_compose, 
                       compose_version: '1.14.0', 
                       yml: "/home/vagrant/workspace/docker-compose.yml",
                       run: "always"  

end