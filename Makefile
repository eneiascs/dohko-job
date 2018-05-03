UBUNTU_IMAGE_NAME="eneiascs/dohko-job-ubuntu:1.0.0"
CENTOS_IMAGE_NAME="eneiascs/dohko-job-centos:1.0.0"

create-ubuntu:
	make clean-ubuntu
	mvn clean package -U
	cp Vagrantfile ubuntu/Vagrantfile
	cp run ubuntu/run	
	cp -R target ubuntu/target
	cd ubuntu; docker build --no-cache -t ${UBUNTU_IMAGE_NAME} . 
	
clean-ubuntu:
	rm -f ubuntu/Vagrantfile
	rm -f ubuntu/run 
	rm -rf ubuntu/target
	make rmi-ubuntu

rmi-ubuntu:
	[ -z $(docker images -q ${UBUNTU_IMAGE_NAME}) ] || docker rmi -f ${UBUNTU_IMAGE_NAME}

create-centos:
	make clean-centos
	mvn clean package -U
	cp Vagrantfile centos/Vagrantfile
	cp run centos/run
	cp -R target centos/target
	cd centos; docker build --no-cache -t ${CENTOS_IMAGE_NAME} . 
	
clean-centos:
	rm -f centos/Vagrantfile
	rm -f centos/run 
	rm -rf centos/target
	make rmi-centos

rmi-centos:
	[ -z $(docker images -q ${CENTOS_IMAGE_NAME}) ] || docker rmi -f ${CENTOS_IMAGE_NAME}	

clean-all:
	make clean-ubuntu
	make clean-centos

