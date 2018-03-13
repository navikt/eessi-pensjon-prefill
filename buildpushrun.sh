#!/usr/bin/env bash
REPO="repo.adeo.no:5443"
APP_NAME="eessi-fagmodul"
NUM=$(cat ./version)
ARTIFACT_VERSION="0.1-SNAPSHOT"
VERSION="${ARTIFACT_VERSION}-${NUM}"


ENV=${1:-t1}
NS=${2:-t1}

mvn clean install -DskipTests

# Bump snapshot-version
echo $((NUM + 1)) > ./version

# build & push
docker build . -t ${REPO}/${APP_NAME}:${VERSION}
docker push ${REPO}/${APP_NAME}:${VERSION}

# stop old and run new
docker stop $(docker ps | grep "${APP_NAME}:${ARTIFACT_VERSION}" | sed 's/.* //g')
docker run -d -p :8080 ${REPO}/${APP_NAME}:${VERSION}

# print new container name
docker ps | grep ${APP_NAME}



echo "Running nais validate"
nais validate
if [ ! $? -eq 0 ]; then
    echo "Error during nais validate"
    exit
fi

echo "Running nais upload"
nais upload -a ${APP_NAME} -v ${VERSION}
if [ ! $? -eq 0 ]; then
    echo "Error during nais upload"
    exit
fi

echo "Running nais deploy"
nais deploy -a ${APP_NAME} -v ${VERSION} -e ${ENV} -n ${NS} -u ${NAIS_UN} -p ${NAIS_PW} | sed "s/${NAIS_PW}/******/g"
if [ ! ${PIPESTATUS[0]} -eq 0 ]; then
    echo "Error during nais deploy"
    exit
fi

echo "Checking deploy progress"
STATUS="InProgress"
until [ ${STATUS} != "InProgress" ]; do
    JSON_RES=$(curl --silent -k https://daemon.nais.preprod.local/deploystatus/${ENV}/${APP_NAME})
    STATUS=$(echo ${JSON_RES} | python -c "import sys, json; print(json.load(sys.stdin)['Status'])")
    echo ${JSON_RES} | python -c "import sys, json; print(json.load(sys.stdin)['Reason'])"
    sleep 5
done
echo ${JSON_RES}
