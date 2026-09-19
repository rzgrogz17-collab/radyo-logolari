package ogzapp.wordgame.net;

public interface WordMeaningProvider{
    WordMeaningRequest get(String langCode);
}
