FROM erikedin/taut

USER root

COPY requirements.txt /app/requirements.txt
RUN pip3 install -r /app/requirements.txt

COPY . /acceptance

WORKDIR /acceptance

USER user