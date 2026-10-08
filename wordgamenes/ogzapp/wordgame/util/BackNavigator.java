package ogzapp.wordgame.util;


import ogzapp.wordgame.screens.BaseScreen;

public interface BackNavigator {

    void notifyNavigationController(BaseScreen screen);
    boolean navigateBack();
}
