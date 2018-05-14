import numpy as np
import keras as K
import simplejson
import mysql.connector
from flask import Flask, request
import nltk
from nltk.corpus import stopwords
import operator

# nltk.download()

stop_words = set(stopwords.words('english'))

# load model
model = K.models.load_model('model.h5')

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
        return json.dumps({'response': response, 'conditions': '', 'next_symptoms': ''})

    words = nltk.word_tokenize(sentence)
    pos = nltk.pos_tag(words)

    #print(pos)

    prev_pos = pos[0][1]
    term = ""
    terms = []

    for i in range(0, len(words)):
        current_pos = pos[i][1]
        if prev_pos == "JJ" or current_pos.startswith("NN") or current_pos == "JJ" or current_pos.startswith(
                "VB") or current_pos == "RB":
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

    # load symptoms
    symptoms_to_ids = {}
    i = 0
    with open('../data/symptoms.txt') as f:
        for line in f:
            symptoms_to_ids[line.rstrip()] = i
            i += 1

    # load conditions
    ids_to_conditions = {}
    i = 0
    with open('../data/conditions.txt') as f:
        for line in f:
            ids_to_conditions[i] = line.split("|")[0].rstrip()
            i += 1

    input = np.zeros(1290)
    for symptom in symptoms:
        input[symptoms_to_ids[symptom]] = 1

    output_vector_conditions = np.ndarray.round(model.predict(np.array([input, ]))[0])

    i = 0
    conditions = set()
    next_symptoms = {}
    for out in output_vector_conditions:
        if out == 1:
            condition = ids_to_conditions[i]
            query = 'SELECT name, symptoms, MATCH(name) AGAINST(\'' + condition + '\') AS score FROM conditions WHERE MATCH(name) AGAINST(\'' + condition + '\') HAVING score > 5 LIMIT 1;'
            cursor.execute(query)
            data = cursor.fetchall()
            for row in data:
                conditions.add(row[0])
                for e in row[1].replace('\u2013', '-').rstrip(', ').split(', '):
                    if e != '':
                        query = 'SELECT popularity FROM symptoms WHERE MATCH(symptom) AGAINST(\'' + e.replace('\'', '') + '\') LIMIT 1;'
                        cursor.execute(query)
                        data2 = cursor.fetchall()
                        if len(data2) == 1:
                            popularity = data2[0][0]
                            next_symptoms[e] = popularity
        i += 1

    conditions = list(conditions)

    symptoms = sorted(next_symptoms.items(), key=operator.itemgetter(1), reverse=True)

    return simplejson.dumps({'response': response, 'conditions': conditions, 'next_symptoms': symptoms})


if __name__ == '__main__':
    app.run(host='0.0.0.0')
