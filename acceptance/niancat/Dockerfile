FROM erikedin/taut

COPY requirements.txt /acceptance/requirements.txt
RUN pip3 install -r /acceptance/requirements.txt

COPY . /acceptance

WORKDIR /acceptance

USER user