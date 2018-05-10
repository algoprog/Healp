import nltk
from nltk.corpus import stopwords

#nltk.download()

stop_words = set(stopwords.words('english'))


def getUserVar(var, profile):
    i = 0
    while i < len(profile):
        if profile[i][0] == var:
            return profile[i][1]
        i += 1

# user profile: name, age, gender, weight, height, country
def respond(sentence, profile=[]):
    if sentence == "":
        if not profile:
            response = "Hi, I am Healp, your personal health assistant. Before you start chatting with me I would " \
                       "like to know some things about you."
        else:
            response = "Hi " + getUserVar("name", profile) + "! How can I help you today?"
        return response

    words = nltk.word_tokenize(sentence)
    pos = nltk.pos_tag(words)

    #print(pos)

    prev_pos = pos[0][1]
    term = ""
    terms = []

    for i in range(0, len(words)):
        current_pos = pos[i][1]
        if prev_pos == "JJ" or current_pos.startswith("NN") or current_pos == "JJ" or current_pos.startswith("VB"):
            if current_pos == "VBZ":
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

    return response


#r = respond("I have stomachache, severe headache and also my nose is bleeding.")
r = respond("", [("name", "Chris"),
                 ("age", 22),
                 ("gender", "male"),
                 ("weight", 59),
                 ("height", 180),
                 ("country", "Greece")])
#r = respond("")

print(r)
