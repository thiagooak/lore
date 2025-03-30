# Lore Documentation

Reference article: [Learn-B: a social analytics-enabled tool for self-regulated workplace learning](https://dl.acm.org/doi/abs/10.1145/2330601.2330632)

Miro [board](https://miro.com/app/board/uXjVIJHbNbo=/)

```mermaid
graph TD;
    User-->Persona;
    Persona-->Manager;
    Persona-->Learner;
    Persona-->Buddy;
    Learner-->Learning-Objective;
    Course-->Learning-Objective;
    Learner-->Journey;
    Learner-->Course;
    Journey-->Course;
    Course-->Module;
    Learner-->Feedback;
    Feedback-->Course;
    Feedback-->Module;
```