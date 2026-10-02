package ogzapp.wordgame;


import ogzapp.wordgame.net.WordMeaningProvider;
import ogzapp.wordgame.net.WordMeaningRequest;

public class WordMeaningProviderAndroid implements WordMeaningProvider {


    public WordMeaningRequest get(String langCode){
        if(langCode.equals("en")) return new WordMeaningRequest_en();

        return null;
    }

}
