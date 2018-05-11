import numpy
import pandas
from keras.models import Sequential
from keras.layers import Dense, BatchNormalization
from keras.optimizers import Adam
from keras.utils import np_utils
from sklearn.preprocessing import LabelEncoder
import matplotlib.pyplot as plt

# fix random seed for reproducibility
seed = 7
numpy.random.seed(seed)

# load dataset
dataframe = pandas.read_csv("../data/data.txt", header=None)
dataset = dataframe.values
X = dataset[:, 0:466].astype(float)
Y = dataset[:, 466:1190]


# define baseline model
def baseline_model():
    # create model
    model = Sequential()
    model.add(Dense(200, input_dim=466, activation='relu'))
    #model.add(BatchNormalization())
    model.add(Dense(100, activation='relu'))
    #model.add(BatchNormalization())
    model.add(Dense(724, activation='softmax'))
    # Compile model
    model.compile(loss='categorical_crossentropy', optimizer=Adam(lr=0.01), metrics=['accuracy'])
    return model


model = baseline_model();

# Fit the model
history = model.fit(X, Y, epochs=100, batch_size=10, verbose=1)

# save model
model.save('my_model.h5')

# summarize history for accuracy
plt.plot(history.history['acc'])
plt.title('model accuracy')
plt.ylabel('accuracy')
plt.xlabel('epoch')
plt.legend(['train'], loc='upper left')
plt.show()

# summarize history for loss
plt.plot(history.history['loss'])
plt.title('model loss')
plt.ylabel('loss')
plt.xlabel('epoch')
plt.legend(['train'], loc='upper left')
plt.show()
