package org.projectforge.launcher.config;

import java.io.File;

import org.apache.commons.lang.StringUtils;

import de.micromata.mgc.javafx.ModelGuiField;
import de.micromata.mgc.javafx.launcher.gui.AbstractConfigTabController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class PfBaseSettingsController extends AbstractConfigTabController<PfBasicLocalSettingsConfigModel>
{
  @FXML
  @ModelGuiField
  private TextField baseDir;
  @FXML
  private Button baseDirSelectButton;
  @FXML
  @ModelGuiField
  private CheckBox projectforgeTestsystemMode;
  @FXML
  @ModelGuiField
  private CheckBox projectforgeWicketDevelopmentMode;

  private File getDefaultStorageFilePath()
  {
    if (StringUtils.isBlank(baseDir.getText()) == true) {
      return new File(".");
    }
    return new File(baseDir.getText());
  }

  @Override
  public void initializeWithModel()
  {
    fromModel();

    baseDirSelectButton.setOnAction(e -> {
      DirectoryChooser fileChooser = new DirectoryChooser();
      fileChooser.setInitialDirectory(getDefaultStorageFilePath());
      File res = fileChooser.showDialog(getConfigDialog().getStage());
      if (res != null) {
        baseDir.setText(res.getAbsolutePath());
      }
    });
  }

  @Override
  public void fromModel()
  {
    super.fromModel();

  }

  @Override
  public void toModel()
  {
    super.toModel();
  }

  @Override
  public String getTabTitle()
  {
    return "PF Basics";
  }

}
