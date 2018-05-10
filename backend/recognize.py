import nltk
from nltk.corpus import stopwords

# nltk.download()

stop_words = set(stopwords.words('english'))

sentence = "I have severe headaches and nose bleeding every day"

words = nltk.word_tokenize(sentence)
pos = nltk.pos_tag(words)

# print(pos)

prev_pos = pos[0][1]
term = ""
terms = []

for i in range(0, len(words)):
    current_pos = pos[i][1]
    if prev_pos == "JJ" or (current_pos.startswith("NN") or current_pos == "JJ"):
        prev_pos = current_pos
        if not words[i] in stop_words:
            term += " " + words[i]
    else:
        terms.append(term.strip())
        term = ""
if term != "":
    terms.append(term.strip())

for term in terms:
    print(term+"\n")
