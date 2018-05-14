import decimal

import simplejson
import mysql.connector
from flask import Flask, request
import nltk
from nltk.corpus import stopwords
import operator

# nltk.download()

stop_words = set(stopwords.words('english'))

# connect to MySQL database
conn = mysql.connector.connect(host='localhost', user='root', db='healp', passwd='')
cursor = conn.cursor()

app = Flask(__name__)


def getVar(var):
    return request.args.get(var, '')


@app.route('/api', methods=['GET'])
def respond():
    sentence = getVar('query')

    if sentence == '':
        if not getVar('name'):
            response = "Hi, I am Healp, your personal health assistant. Before you start chatting with me I would " \
                       "like to know some things about you."
        else:
            response = "Hi " + getVar("name") + "! How can I help you today?"
        return simplejson.dumps({'response': response, 'conditions': '', 'next_symptoms': ''})

    words = nltk.word_tokenize(sentence)
    pos = nltk.pos_tag(words)

    print(pos)

    prev_pos = pos[0][1]
    term = ""
    terms = []

    for i in range(0, len(words)):
        current_pos = pos[i][1]
        if current_pos == "CC":
            term = term.strip()
            if term != "":
                terms.append(term.strip())
            term = ""
        elif prev_pos == "JJ" or current_pos.startswith("NN") or current_pos == "JJ" or current_pos.startswith("VB") or current_pos == "RB":
            if current_pos == "VBZ" or current_pos == "VBP":
                continue
            prev_pos = current_pos
            if not words[i] in stop_words:
                term += " " + words[i]
        else:
            term = term.strip()
            if term != "":
                terms.append(term.strip())
            term = ""
    if term != "":
        terms.append(term.strip())

    response = "I see that you have "

    i = 1
    for term in terms:
        if i == len(terms):
            ending = "."
        elif i == len(terms) - 1:
            ending = " and "
        else:
            ending = ", "
        response += term + ending
        i += 1

    symptoms = terms

    conditions = []

    query = 'SELECT name, symptoms, MATCH(symptoms) AGAINST(\'' + ', '.join(symptoms) + '\') AS score FROM conditions WHERE MATCH(symptoms) AGAINST(\'' + ', '.join(symptoms) + '\');'
    cursor.execute(query)
    data = cursor.fetchall()

    next_symptoms = {}
    for row in data:
        score = row[2]
        conditions.append({'condition': row[0], 'score': score})
        for e in row[1].replace('\u2013', '-').rstrip(', ').split(', '):
            if e != '':
                query = 'SELECT popularity FROM symptoms WHERE MATCH(symptom) AGAINST(\'' + e.replace('\'', '') + '\') LIMIT 1;'
                cursor.execute(query)
                data2 = cursor.fetchall()
                if len(data2) == 1:
                    popularity = data2[0][0]
                    next_symptoms[e] = popularity*decimal.Decimal(score)

    symptoms = sorted(next_symptoms.items(), key=operator.itemgetter(1), reverse=True)

    return simplejson.dumps({'response': response, 'conditions': conditions, 'next_symptoms': symptoms})


if __name__ == '__main__':
    app.run(host='0.0.0.0')
