FROM alpine
LABEL maintainer='Erik Edin <erikedin.name@gmail.com>'

ENV PYTHONUNBUFFERED=0

RUN apk update && apk add python3 openssl

WORKDIR /var/cert
RUN openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 365 -subj '/CN=slack.com' -nodes -sha256

COPY requirements.txt /slackrest/requirements.txt
RUN pip3 install -r /slackrest/requirements.txt
COPY . /slackrest/
WORKDIR /slackrest
RUN python3 setup.py install


#ENV REQUESTS_CA_BUNDLE=/var/cert/cert.pem
#ENV WEBSOCKET_CLIENT_CA_BUNDLE=/var/cert/cert.pem