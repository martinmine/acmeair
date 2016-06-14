REGISTRY=registry.ng.bluemix.net
NAME_SPACE=
MONGO_BRIDGE=
SD_URL=https://servicediscovery.ng.bluemix.net
SD_TOKEN=

docker build -f ./acmeair-mainapp/Dockerfile_BlueMix_main -t ${REGISTRY}/${NAME_SPACE}/acmeair_java_mainservice .
docker build -f ./acmeair-as/Dockerfile_BlueMix_as -t ${REGISTRY}/${NAME_SPACE}/acmeair_java_authservice .
docker build -f ./acmeair-bs/Dockerfile_BlueMix_bs -t ${REGISTRY}/${NAME_SPACE}/acmeair_java_bookingservice .
docker build -f ./acmeair-cs/Dockerfile_BlueMix_cs -t ${REGISTRY}/${NAME_SPACE}/acmeair_java_customerservice .
docker build -f ./acmeair-fs/Dockerfile_BlueMix_fs -t ${REGISTRY}/${NAME_SPACE}/acmeair_java_flightservice .


docker tag  -f acmeair_java_mainservice:latest ${REGISTRY}/${NAME_SPACE}/acmeair_java_mainservice:latest
docker tag -f acmeair_java_authservice:latest ${REGISTRY}/${NAME_SPACE}/acmeair_java_authservice:latest
docker tag -f acmeair_java_bookingservice:latest ${REGISTRY}/${NAME_SPACE}/acmeair_java_bookingservice:latest
docker tag -f acmeair_java_customerservice:latest ${REGISTRY}/${NAME_SPACE}/acmeair_java_customerservice:latest
docker tag -f acmeair_java_flightservice:latest ${REGISTRY}/${NAME_SPACE}/acmeair_java_flightservice:latest

docker push ${REGISTRY}/${NAME_SPACE}/acmeair_java_mainservice
docker push ${REGISTRY}/${NAME_SPACE}/acmeair_java_authservice
docker push ${REGISTRY}/${NAME_SPACE}/acmeair_java_bookingservice
docker push ${REGISTRY}/${NAME_SPACE}/acmeair_java_customerservice
docker push ${REGISTRY}/${NAME_SPACE}/acmeair_java_flightservice

cf ic run -e SERVICE_NAME=main -e SD_URL=${SD_URL} -e SD_TOKEN=${SD_TOKEN} --name main_2 ${REGISTRY}/${NAME_SPACE}/acmeair_java_mainservice
cf ic run -e CCS_BIND_APP=${MONGO_BRIDGE} -e SERVICE_NAME=auth     -e SD_URL=${SD_URL} -e SD_TOKEN=${SD_TOKEN} --name auth_2     ${REGISTRY}/${NAME_SPACE}/acmeair_java_authservice
cf ic run -e CCS_BIND_APP=${MONGO_BRIDGE} -e SERVICE_NAME=booking  -e SD_URL=${SD_URL} -e SD_TOKEN=${SD_TOKEN} --name booking_2  ${REGISTRY}/${NAME_SPACE}/acmeair_java_bookingservice
cf ic run -e CCS_BIND_APP=${MONGO_BRIDGE} -e SERVICE_NAME=customer -e SD_URL=${SD_URL} -e SD_TOKEN=${SD_TOKEN} --name customer_2 ${REGISTRY}/${NAME_SPACE}/acmeair_java_customerservice
cf ic run -e CCS_BIND_APP=${MONGO_BRIDGE} -e SERVICE_NAME=flight   -e SD_URL=${SD_URL} -e SD_TOKEN=${SD_TOKEN} --name flight_2   ${REGISTRY}/${NAME_SPACE}/acmeair_java_flightservice
