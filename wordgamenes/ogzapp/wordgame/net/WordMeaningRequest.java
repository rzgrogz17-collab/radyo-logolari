package ogzapp.wordgame.net;


import ogzapp.wordgame.ui.dialogs.DictionaryDialog;

public interface WordMeaningRequest {
    void request(String word, DictionaryDialog.DictionaryCallback callback);
}
