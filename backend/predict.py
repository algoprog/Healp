import numpy as np
import keras as K
import json
import mysql.connector
from flask import Flask, request

# load model
model = K.models.load_model('model.h5')

# connect to MySQL database
conn = mysql.connector.connect(host='localhost', user='root', db='healp', passwd='')
cursor = conn.cursor()

app = Flask(__name__)


@app.route('/predict', methods=['GET'])
def predict_conditions():
    symptoms = request.args.get('symptoms', '').split(',')

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
    next_symptoms = []
    for out in output_vector_conditions:
        if out == 1:
            condition = ids_to_conditions[i]
            query = 'SELECT name, symptoms, MATCH(name) AGAINST(\''+condition+'\') AS score FROM conditions WHERE MATCH(name) AGAINST(\''+condition+'\') HAVING score > 5 LIMIT 1;'
            cursor.execute(query)
            data = cursor.fetchall()
            for row in data:
                conditions.add(row[0])
                for e in row[1].replace('\u2013', '-').rstrip(', ').split(', '):
                    if e != '':
                        next_symptoms.append(e)
        i += 1
    conditions = list(conditions)

    return json.dumps({'conditions': conditions, 'next_symptoms': next_symptoms})


if __name__ == '__main__':
    app.run(host='0.0.0.0')
