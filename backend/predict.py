import numpy as np
import keras as K
import json
import operator
from flask import Flask, request

# load model
model = K.models.load_model('model.h5')

app = Flask(__name__)


@app.route('/predict', methods=['GET'])
def predict_conditions():
    symptoms = request.args.get('symptoms', '').split(',')

    # load symptoms
    symptoms_to_ids = {}
    ids_to_symptoms = {}
    i = 0
    with open('../data/symptoms.txt') as f:
        for line in f:
            symptoms_to_ids[line.rstrip()] = i
            ids_to_symptoms[i] = line.rstrip()
            i += 1

    conditions_symptoms = {}
    with open('../data/conditions_symptoms_2.txt') as f:
        for line in f:
            parts = line.split('::')
            condition = parts[0]
            c_symptoms = parts[1].rstrip().rstrip('|').split('|')
            conditions_symptoms[condition] = c_symptoms
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
    conditions = []
    for out in output_vector_conditions:
        if out == 1:
            conditions.append(ids_to_conditions[i])
        i += 1

    '''
    next_symptoms = {}
    symptoms_counts = {}
    for condition in conditions:
        for symptom in conditions_symptoms[condition]:
            if symptom not in symptoms:
                if symptom not in symptoms_counts:
                    symptoms_counts[symptom] = 1
                else:
                    symptoms_counts[symptom] += 1

    sorted_counts = sorted(symptoms_counts.items(), key=operator.itemgetter(1), reverse=True)

    for symptom_count in sorted_counts:
        next_symptoms[symptom_count[0]] = symptom_count[1]
    '''

    return json.dumps({'conditions': conditions})


if __name__ == '__main__':
    app.run(host='0.0.0.0')
