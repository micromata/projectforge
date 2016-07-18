package org.projectforge.launcher;

import de.micromata.mgc.javafx.FXEvents;
import de.micromata.mgc.javafx.FXMLFile;
import de.micromata.mgc.javafx.launcher.MgcLauncherEvent;
import de.micromata.mgc.javafx.launcher.gui.generic.GenericMainWindow;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.projectforge.launcher.config.PfLocalSettingsConfigModel;

@FXMLFile(file = "/fxml/GenericMainWindow.fxml")
public class PfMainWindow extends GenericMainWindow<PfLocalSettingsConfigModel>
{

  @Override
  protected void addStopServerEventHandler()
  {
    FXEvents.get().addEventHandler(this, stopServerButton, MgcLauncherEvent.APP_STOPPED, event -> {
      startServerButton.setDisable(false);
      stopServerButton.setDisable(true);
    });
  }

  @Override
  public void stopServer()
  {
    super.stopServer();
    Alert alert = new Alert(AlertType.WARNING);
    alert.setTitle("Restart Application");
    alert.setHeaderText("Warning");
    alert.setContentText("Before you restart ProjectForge Server you have to restart the Launcher");
    alert.showAndWait();
    startServerButton.setDisable(true);

  }

}
