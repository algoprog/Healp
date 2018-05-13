import numpy
import pandas
from keras.callbacks import ModelCheckpoint
from keras.models import Sequential
from keras.layers import Dense, BatchNormalization, Dropout
from keras.optimizers import Adam
import matplotlib.pyplot as plt

# fix random seed for reproducibility
seed = 7
numpy.random.seed(seed)

# load dataset
dataframe = pandas.read_csv('../data/data.txt', header=None)
dataset = dataframe.values
X = dataset[:, 0:1290].astype(int)
Y = dataset[:, 1290:2014].astype(int)


# define baseline model
def neural_model():
    # create model
    model = Sequential()
    model.add(Dense(1000, input_dim=1290, activation='relu'))
    model.add(Dense(724, activation='sigmoid'))
    # Compile model
    model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy', 'mae'])
    return model


model = neural_model()

checkpointer = ModelCheckpoint(filepath='model.h5', verbose=1)

# Fit the model
history = model.fit(X, Y, epochs=20, batch_size=10, shuffle=True, verbose=1, callbacks=[checkpointer])

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
