package com.sevilinma.tools.jscompiler;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.SourceFile;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    public BorderPane mainFormPane;
    public TextField outputDirPath;
    public ListView<String> listView;

    private File outputDirPathFile;
    private ContextMenu contextMenu;

    public MainController() {
        contextMenu = new ContextMenu();
        MenuItem delete = new MenuItem("Remove");
        delete.setOnAction(event -> {
            listView.getItems().remove(listView.getSelectionModel().getSelectedItem());
        });
        MenuItem compiler = new MenuItem("Compression");
        compiler.setOnAction(event -> {
            if(outputDirPathFile != null){
                String fullPath = listView.getSelectionModel().getSelectedItem();
                File file = new File(fullPath);
                String outFile = outputDirPathFile.getAbsolutePath() + File.separator + file.getName().replace(".js",".min.js");
                compileJSFile(fullPath, outFile);
                showMessage("The JS file compression is complete.");
            }
        });
        contextMenu.getItems().add(delete);
        contextMenu.getItems().add(compiler);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if(listView != null){
            listView.setOnDragOver(event -> {
                if (event.getGestureSource() != listView) {
                    event.acceptTransferModes(TransferMode.ANY);
                }
            });
            listView.setOnDragDropped(event -> {
                Dragboard dragboard = event.getDragboard();
                List<File> files = dragboard.getFiles();
                if(files.size() > 0){
                    try {
                        files.forEach(file -> {
                            if(file.isFile()) {
                                String fullPath = file.getAbsolutePath();
                                if (fullPath.endsWith(".js") && !fullPath.endsWith(".min.js")) {
                                    listView.getItems().add(fullPath);
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            listView.setOnMouseClicked(event -> {
                if(event.getButton()== MouseButton.SECONDARY && !listView.getSelectionModel().isEmpty()){
                    contextMenu.show(listView, event.getScreenX(), event.getScreenY());
                }else{
                    contextMenu.hide();
                }
            });
        }
    }

    @FXML
    private void OnCompileAction(){
        if(outputDirPathFile != null){
            if(listView != null && !listView.getItems().isEmpty()){
                listView.getItems().forEach(fullPath -> {
                    System.out.println("Compiler file:"+fullPath);
                    File file = new File(fullPath);
                    String outFile = outputDirPathFile.getAbsolutePath() + File.separator + file.getName().replace(".js",".min.js");
                    compileJSFile(fullPath, outFile);
                });
                showMessage("The JS file compression is complete.");
            }
        }else{
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,"If the output directory is not specified, it will be automatically output in the source file directory.", ButtonType.YES, ButtonType.NO);
            a.setTitle("提示");
            a.setHeaderText("");
            Optional<ButtonType> bt = a.showAndWait();
            if(bt.get() == ButtonType.YES){
                if(listView != null && !listView.getItems().isEmpty()){
                    listView.getItems().forEach(fullPath -> {
                        System.out.println("Compiler file:"+fullPath);
                        File file = new File(fullPath);
                        String outFile = file.getParentFile().getAbsolutePath() + File.separator + file.getName().replace(".js",".min.js");
                        compileJSFile(fullPath, outFile);
                    });
                }
                showMessage("The JS file compression is complete.");
            }
        }
    }

    @FXML
    private void OnChooseAction(){
        DirectoryChooser dc = new DirectoryChooser();
        outputDirPathFile = dc.showDialog(mainFormPane.getScene().getWindow());
        if(outputDirPathFile != null){
            System.out.println("Choose:" + outputDirPathFile.getAbsolutePath());
            outputDirPath.setText(outputDirPathFile.getAbsolutePath());
        }
    }

    private void compileJSFile(String origin, String target){
        try {
            File file = new File(origin);
            if(file.exists() && file.isFile()){
                String content = new String(Files.readAllBytes(Paths.get(origin)),StandardCharsets.UTF_8);
                content = compileJs(content);
                Files.write(Paths.get(target),content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Compiler file: Success");
            }else{
                showMessage("Compiler file: No Found");
                System.out.println("Compiler file: No Found");
            }
        }catch (Exception e){
            e.printStackTrace();
            showMessage("Compiler file: Failed");
            System.out.println("Compiler file: Failed");
        }
    }

    private void showMessage(String message){
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information");
        a.setHeaderText("");
        a.setContentText(message);
        a.showAndWait();
    }
    /**
     * 校验js语法、压缩js
     * @param code 代码字符串
     * @return 压缩后的字符串
     */
    private String compileJs(String code){
        if (code==null || code.equals("")) {
            System.out.println("this code file is empty");
            return null;
        }

        com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler();

        CompilerOptions options = new CompilerOptions();
        // Simple mode is used here, but additional options could be set, too.
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);

        // To get the complete set of externs, the logic in
        // CompilerRunner.getDefaultExterns() should be used here.
        SourceFile extern = SourceFile.fromCode("externs.js",
                "function alert(x) {}");

        // The dummy input name "input.js" is used here so that any warnings or
        // errors will cite line numbers in terms of input.js.
        SourceFile input = SourceFile.fromCode("input.js", code);

        // compile() returns a Result, but it is not needed here.
        compiler.compile(extern, input, options);

        // The compiler is responsible for generating the compiled code; it is not
        // accessible via the Result.
        if(compiler.getErrorCount() > 0){
            StringBuilder erroInfo = new StringBuilder();
            for(JSError jsError: compiler.getErrors()) {
                erroInfo.append(jsError.toString());
            }
            System.out.println(erroInfo);
            return null;
        }
        return compiler.toSource();
    }
}
