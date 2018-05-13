import numpy as np
import pandas
import keras as K
import matplotlib.pyplot as plt


symptoms = ["Lack of motivation", "Lack of emotion", "Lack of pleasure"]

# load model
model = K.models.load_model("model.h5")

# load symptoms
symptoms_to_ids = {}
i = 0
with open("../data/symptoms.txt") as f:
    for line in f:
        symptoms_to_ids[line.rstrip()] = i
        i += 1

# load conditions
ids_to_conditions = {}
i = 0
with open("../data/conditions.txt") as f:
    for line in f:
        ids_to_conditions[i] = line.split("|")[0].rstrip()
        i += 1

input_vector = np.zeros(1290)
for symptom in symptoms:
    input_vector[symptoms_to_ids[symptom]] = 1

output_vector = np.ndarray.round(model.predict(np.array([input_vector, ])))[0]

i = 0
conditions = []
for out in output_vector:
    if out == 1:
        conditions.append(ids_to_conditions[i])
    i += 1

print(conditions)
