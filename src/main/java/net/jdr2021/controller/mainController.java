package net.jdr2021.controller;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.jdr2021.utils.*;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.jdr2021.main.PrimaryStage;

/**
 * @version 1.0
 * @Author jdr
 * @Date 2024-5-23 14:25
 * @注释
 */

public class mainController {


    @FXML
    private TextField tf_kkfileview_url, tf_oss_url;
    @FXML
    private CheckBox tf_oss_ignoreSSLErr;
    @FXML
    private WebView webView;

    @FXML
    private Label treeStatsLabel;
    @FXML
    private TreeView treeView;
    @FXML
    private TreeItem<String> root;

    private XMLParser.File[] keys;
    private String[] fileUrls; // 存储每个文件的完整 URL
    private int currentIndex = 0;

    //String imageExtensions = ConfigLoader.getProperty("image.extensions");
    String allowExtensions = ConfigLoader.getProperty("allow.extensions");
    String kkFileView_URL = ConfigLoader.getProperty("kkFileView_URL");
    String api = "/onlinePreview?url=";

    // 获取系统剪贴板
    public static Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    //初始化
    @FXML
    private void initialize() {
        if (kkFileView_URL.endsWith("/")) {
            kkFileView_URL = kkFileView_URL;
        } else {
            kkFileView_URL = kkFileView_URL + "/";
        }
        tf_kkfileview_url.setText(kkFileView_URL);

        // 1. 支持多选
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 2. 添加右键菜单
        final ContextMenu contextMenu = new ContextMenu();

        MenuItem addItem0 = new MenuItem("保存");
        MenuItem addItem = new MenuItem("批量保存");
        MenuItem deleteItem = new MenuItem("删除");
        MenuItem copyItem = new MenuItem("复制URL");

        contextMenu.getItems().addAll(copyItem, addItem0, addItem
//                deleteItem,  //暂不支持，要不然会破坏上一个和下一个的索引
        );

        addItem0.setOnAction(e -> {
            SaveFile();
        });
        // 批量保存事件
        addItem.setOnAction(e -> {

            if (keys == null && keys.length == 0) {
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("警告");
            alert.setHeaderText(null); // 可以不显示标题
            alert.setContentText("请注意是否为渗透测试，保存所有数据是否合法？");

            Optional<ButtonType> result = alert.showAndWait();


            if (!(result.isPresent() && result.get() == ButtonType.OK)) {
                return;
            }

            ObservableList<XMLParser.File> selected = treeView.getSelectionModel().getSelectedItems();
            if (selected != null && selected.size() > 0) {

                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("选择文件夹");

                // 可选：设置初始目录
                directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

                File selectedDirectory = directoryChooser.showDialog(PrimaryStage);

                if (selectedDirectory != null) {
                    String saveDir = selectedDirectory.getAbsolutePath();
                    System.out.println("选择的文件夹路径：" + saveDir);

                    String fname = "";
                    XMLParser.File file;

                    for (int i = 0; i < selected.size(); i++) {
                        file = selected.get(i);
                        fname = file.getPath();
                        fname = fname.substring(fname.lastIndexOf('/') + 1);
                        fname = fname.substring(0, fname.lastIndexOf(' '));
                        saveFileBase(tf_oss_url.getText() + file.getPath(),
                                saveDir + "/" + fname);
                    }


                    if (selected.size() == 1) {
                        try {
                            openFileDirectory(saveDir + "/" + fname);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }

            }
        });

        // 删除事件（支持多选）
        deleteItem.setOnAction(e -> {
            ObservableList<TreeItem<String>> selectedItems = treeView.getSelectionModel().getSelectedItems();
            for (TreeItem<String> item : selectedItems) {
                if (item.getParent() != null) {
                    item.getParent().getChildren().remove(item);

//                    数组里面的元素也要删除

                }
            }
        });

        copyItem.setOnAction(e -> {

            TreeItem<String> selected = (TreeItem<String>) treeView.getSelectionModel().getSelectedItem();
            if (selected != null) {

                String selected1 = selected.getValue();
                selected1 = selected1.substring(selected1.indexOf('/'));
// 把内容放入剪贴板
                clipboard.setContents(new StringSelection(remove(tf_oss_url.getText()) +
                                selected1),
                        null);

            }
        });


        // 右键菜单绑定
        treeView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                TreeItem<String> selected = (TreeItem<String>) treeView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    contextMenu.show(treeView, event.getScreenX(), event.getScreenY());
                }
            } else {
                contextMenu.hide();
            }
        });

        // 自定义 cellFactory，为每个 cell 设置 tooltip
        treeView.setCellFactory(new Callback<TreeView<Object>, TreeCell<Object>>() {
            @Override
            public TreeCell<Object> call(TreeView<Object> tv) {
                final TreeCell<Object> cell = new TreeCell<Object>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {  //自己绘制元素，并显示tooltip
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                            setTooltip(null);
                        } else {
                            // 文本显示：如果是自定义对象并有 getName/getTitle 可优先显示 name，否则使用 toString()
                            String displayText = deriveDisplayText(item);

                            setText(displayText);

                            // Tooltip 文本：优先尝试 getTitle()，其次显示 toString()
                            String tooltipText = deriveTooltipText(item);
                            if (tooltipText != null && !tooltipText.isEmpty()) {
                                Tooltip tt = new Tooltip(tooltipText);
                                setTooltip(tt);
                            } else {
                                setTooltip(null);
                            }
                        }
                    }
                };


                return cell;
            }
        });
    }

    // 尝试获得显示文本（优先 name/getName，再 fallback toString）
    private String deriveDisplayText(Object item) {
        if (item == null) return "";
        // 尝试常见方法 getName()
        try {
            Method m = item.getClass().getMethod("getName");
            Object v = m.invoke(item);
            if (v != null) return v.toString();
        } catch (Exception ignored) {
        }

        // 否则返回 toString()
        return item.toString();
    }

    // 尝试获得 tooltip 文本（优先 getTitle()，其次 getDescription()，最后 toString()）
    private String deriveTooltipText(Object item) {
        if (item == null) return null;

        // 1) 尝试 getTitle()
        try {
            Method m = item.getClass().getMethod("getTitle");
            Object v = m.invoke(item);
            if (v != null) return v.toString();
        } catch (Exception ignored) {
        }

        // 2) 尝试 getDescription()
        try {
            Method m = item.getClass().getMethod("getDescription");
            Object v = m.invoke(item);
            if (v != null) return v.toString();
        } catch (Exception ignored) {
        }

        // 3) 如果是 String，则不重复显示（可按需决定）
        if (item instanceof String) {
            // 这里我们让 tooltip 显示与文本相同（如果你不想这样可以 return null）
            return item.toString();
        }

        // 4) fallback to toString()
        return item.toString();
    }

    @FXML
    protected void LoadkkFileView() throws Exception {
        String kkurl = tf_kkfileview_url.getText();
        if (kkurl.equals("")) {
            showAlert(Alert.AlertType.ERROR, "错误", null, "kkFileView 地址不能为空！");
            return;
        }
        webView.getEngine().load(kkurl);
    }

    @FXML
    protected void Loading() throws Exception {
        final String oss_url0 = tf_oss_url.getText();
        if (oss_url0.equals("")) {
            showAlert(Alert.AlertType.ERROR, "错误", null, "存储桶地址不能为空！");
            return;
        }

        CompletableFuture.supplyAsync(() -> {

            try {
                return HttpUtils.httpGet(oss_url0, tf_oss_ignoreSSLErr.isSelected());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }).thenAccept(responseData -> {

            String oss_url;
            if (oss_url0.endsWith("/")) {
                oss_url = oss_url0;
            } else {
                oss_url = oss_url0 + "/";
            }
            System.out.println(responseData);
            if (XMLParser.isXMLData(responseData)) {
                if (XMLParser.containsListBucketResult(responseData)) {
                    keys = XMLParser.extractKeys(responseData);
//                for (String key : keys) {
//                    System.out.println(tf_kkfileview_url.getText()+key);
//                }
                    if (keys.length > 0) {
                        constructFileUrls(oss_url);
                        loadNextFile();
                    } else {
                        System.out.println("存储桶为空");
                        showAlert(Alert.AlertType.INFORMATION, "提示", null, "存储桶为空");
                    }
                } else if (XMLParser.containsAccessDenied(responseData)) {
                    // responseData 包含 <Code>AccessDenied</Code> 标签
                    System.out.println("存储桶禁止访问");
                    showAlert(Alert.AlertType.INFORMATION, "提示", null, "存储桶禁止访问");
                } else {
                    // 其他情况
                    System.out.println("不是存储桶");
                    showAlert(Alert.AlertType.INFORMATION, "提示", null, "不是存储桶");
                }
            } else {
                // responseData 不是 XML 格式的数据
                System.out.println("存储桶遍历漏洞不存在");
                showAlert(Alert.AlertType.INFORMATION, "提示", null, "存储桶遍历漏洞不存在");
            }

        });
    }

    @FXML
    protected void PreviousFile() {
        if (fileUrls != null && currentIndex > 0) {
            currentIndex--;
            loadPreviousFile();
        } else {
            System.out.println("已经是第一个文件");
            showAlert(Alert.AlertType.INFORMATION, "提示", null, "已经是第一个文件");
        }
    }

    protected void saveFileBase(String url, String saveDirWithfilename) {

        CompletableFuture.supplyAsync(() -> {

            try {
                HttpUtils.httpGetBinary(url, tf_oss_ignoreSSLErr.isSelected(), saveDirWithfilename);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return null;
        }).thenAccept(responseData -> {


        });

    }

    public void openFileDirectory(String fileName) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("提醒");
        alert.setHeaderText(null); // 可以不显示标题
        alert.setContentText("已保存，是否需要打开所在文件夹？");

        Optional<ButtonType> result = alert.showAndWait();


        if (!(result.isPresent() && result.get() == ButtonType.OK)) {
            return;
        }

        FileUtils.openExplorerAndSelectFile(fileName);
    }

    @FXML
    protected void SaveFile() {

        if (treeView.getSelectionModel() == null ||
                treeView.getSelectionModel().getSelectedItem() == null) {
            return;
        }
        String seletedItemFileName = treeView.getSelectionModel().getSelectedItem().toString();

        if (seletedItemFileName == null) {
            return;
        }
        seletedItemFileName = seletedItemFileName.substring(seletedItemFileName.lastIndexOf('/') + 1);
        seletedItemFileName = seletedItemFileName.substring(0, seletedItemFileName.lastIndexOf(' '));

        // 创建 FileChooser
        FileChooser fileChooser = new FileChooser();

        // 设置窗口标题
        fileChooser.setTitle("选择保存位置和文件名");


        // 设置默认文件名
        fileChooser.setInitialFileName(seletedItemFileName);

        // 设置默认路径
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + File.separator + "Desktop"));

        // 设置可选的文件类型过滤器
//        FileChooser.ExtensionFilter extFilter =
//                new FileChooser.ExtensionFilter("文本文件 (*.txt)", "*.txt");
//        fileChooser.getExtensionFilters().add(extFilter);

        // 弹出保存文件对话框
        File file = fileChooser.showSaveDialog(PrimaryStage);

        if (file != null) {
            System.out.println("保存路径: " + file.getAbsolutePath());
            System.out.println("文件名: " + file.getName());

            saveFileBase(tf_oss_url.getText() + seletedItemFileName,
                    file.getAbsolutePath());

            try {
                openFileDirectory(file.getAbsolutePath());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }


        }

    }

    @FXML
    protected void SaveAllFile() {

        if (keys == null && keys.length == 0) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("警告");
        alert.setHeaderText(null); // 可以不显示标题
        alert.setContentText("请注意是否为渗透测试，保存所有数据是否合法？");

        Optional<ButtonType> result = alert.showAndWait();


        if (!(result.isPresent() && result.get() == ButtonType.OK)) {
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择文件夹");

        // 可选：设置初始目录
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selectedDirectory = directoryChooser.showDialog(PrimaryStage);

        if (selectedDirectory != null) {
            String saveDir = selectedDirectory.getAbsolutePath();
            System.out.println("选择的文件夹路径：" + saveDir);

            String fname = "";
            for (int i = 0; i < keys.length; i++) {
                fname = keys[i].getPath();
                fname = fname.substring(fname.lastIndexOf('/') + 1);
                fname = fname.substring(0, fname.lastIndexOf(' '));
                saveFileBase(tf_oss_url.getText() + keys[i].getPath(),
                        saveDir + "/" + fname);
            }

            if (keys.length == 1) {
                try {
                    openFileDirectory(saveDir + "/" + fname);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

        } else {
            System.out.println("未选择文件夹");
        }
    }


    @FXML
    protected void NextFile() {
        if (fileUrls != null && currentIndex < fileUrls.length - 1) {
            currentIndex++;
            loadNextFile();
        } else {
            System.out.println("已加载所有文件");
            showAlert(Alert.AlertType.INFORMATION, "提示", null, "已加载所有文件");
        }
    }


    public static long previewBottle = 1024 * 1024 * 10;  //10M

    private void loadNextFile() {

        if (tf_kkfileview_url.getText().equals("")) {
            showAlert(Alert.AlertType.ERROR, "错误", null, "kkFileView 地址不能为空！");
            return;
        }

        while (currentIndex < fileUrls.length) {

            if (keys[currentIndex].getSize() > previewBottle) {
                showAlert(Alert.AlertType.INFORMATION, "提示", null, "文件过大，请下载后查看！");
                return;
            }

            String nextFileUrl = fileUrls[currentIndex];

            String extension = getFileExtension(nextFileUrl);

            // 检查 allowExtensions 是否为空
            if (allowExtensions.isEmpty()) {
                // 如果 allowExtensions 为空，则加载所有文件
                System.out.println("加载第" + currentIndex + "个资源：" + nextFileUrl);
                webView.setPrefSize(800, 800);
                webView.getEngine().load(tf_kkfileview_url.getText() + api + Base64Utils.encode(nextFileUrl));
                currentIndex++;
                return;
            } else if (allowExtensions.contains(extension.toLowerCase())) {
                // 如果 allowExtensions 不为空并且包含此扩展名，则加载文件
                System.out.println("加载第" + currentIndex + "个资源：" + nextFileUrl);
                webView.setPrefSize(800, 800);
                webView.getEngine().load(tf_kkfileview_url.getText() + api + Base64Utils.encode(nextFileUrl));
                currentIndex++;
                return;
            } else {
                // 否则加载下一个文件
                currentIndex++;
            }
        }
        System.out.println("已加载所有文件");
        showAlert(Alert.AlertType.INFORMATION, "提示", null, "已加载所有文件");
    }

    private void loadPreviousFile() {
        if (tf_kkfileview_url.getText().equals("")) {
            showAlert(Alert.AlertType.ERROR, "错误", null, "kkFileView 地址不能为空！");
            return;
        }

        while (currentIndex >= 0) {

            if (keys[currentIndex].getSize() > previewBottle) {
                showAlert(Alert.AlertType.INFORMATION, "提示", null, "文件过大，请下载后查看！");
                return;
            }

            String previousFileUrl = fileUrls[currentIndex];
            String extension = getFileExtension(previousFileUrl);

            // 检查 allowExtensions 是否为空
            if (allowExtensions.isEmpty()) {
                // 如果 allowExtensions 为空，则加载所有文件
                System.out.println("加载第" + currentIndex + "个资源：" + previousFileUrl);
                webView.setPrefSize(800, 800);
                webView.getEngine().load(tf_kkfileview_url.getText() + api + Base64Utils.encode(previousFileUrl));
                return;
            } else if (allowExtensions.contains(extension.toLowerCase())) {
                // 如果 allowExtensions 不为空并且包含此扩展名，则加载文件
                System.out.println("加载第" + currentIndex + "个资源：" + previousFileUrl);
                webView.setPrefSize(800, 800);
                webView.getEngine().load(tf_kkfileview_url.getText() + api + Base64Utils.encode(previousFileUrl));
                return;
            } else {
                // 否则加载上一个文件
                currentIndex--;
            }
        }
        System.out.println("已加载所有文件");
        showAlert(Alert.AlertType.INFORMATION, "提示", null, "已加载所有文件");
    }

    // 文件列表
    private void constructFileUrls(String oss_url) {
        fileUrls = new String[keys.length];
        root = new TreeItem<>("Files"); // 创建根节点

        for (int i = 0; i < keys.length; i++) {
            fileUrls[i] = oss_url + keys[i].getPath();
            System.out.println(fileUrls[i]);

            try {
                URL url = new URL(fileUrls[i]);
                String path = url.getPath(); // 获取 URL 的路径部分

                String extension = getFileExtension(path); // 获取文件扩展名

                // 检查 allowExtensions 是否为空
                if (allowExtensions.isEmpty() || allowExtensions.contains(extension.toLowerCase())) {
                    // 如果 allowExtensions 为空或包含此扩展名，则添加文件节点
                    TreeItem<String> fileItem = new TreeItem<>("[" +
                            OtherUtils.formatSize(keys[i].getSize(), true) + "][" + keys[i].getLastModified() +
                            "]——" + keys[i].getPath()); //path 改为 keys[i].getPath(),解决#截断问题
                    root.getChildren().add(fileItem);
                }
            } catch (Exception e) {
                // 处理无法解析 URL 的情况
                e.printStackTrace();
                showAlert(Alert.AlertType.INFORMATION, "提示", null,
                        "无法解析文件列表里的url：" + fileUrls[i]);
            }
        }

        Platform.runLater(() -> {  //主线程修改界面
            treeView.setRoot(root);
            treeView.setShowRoot(false);

            // 添加选择改变监听器
            treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                handleNodeSelected((TreeItem<String>) newValue);
            });

            treeStatsLabel.setText("统计信息: " + keys.length + " 项");
        });
    }

    public static XMLParser.File findItem(XMLParser.File[] array, String target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].getPath().equals(target)) {
                return array[i];
            }
        }
        return null; // 没找到返回 -1
    }


    // 处理节点被选中的方法
    private void handleNodeSelected(TreeItem<String> selectedItem) {
        if (tf_kkfileview_url.getText().equals("")) {
            showAlert(Alert.AlertType.ERROR, "错误", null, "kkFileView 地址不能为空！");
            return;
        }
        if (treeView.getSelectionModel().getSelectedItems().stream().count() > 1) {
            return;  //多选的时候不加载内容
        }

        if (selectedItem != null) {
            String selectedPath = selectedItem.getValue();
            //System.out.println(selectedPath);


            selectedPath = selectedPath.substring(selectedPath.indexOf('/'));
            XMLParser.File c = findItem(keys, selectedPath);
            if (c != null && c.getSize() > previewBottle) {
                showAlert(Alert.AlertType.INFORMATION, "提示", null, "文件过大，请下载后查看！");
                return;
            }


            String url = remove(tf_oss_url.getText()) + selectedPath;
            System.out.println("当前选择是：" + url);
            webView.setPrefSize(800, 800);

            //                        本来想着添加默认后缀，以便kkfileview可以加载（没有后缀，无法识别类型文件）。但是会影响URL，kkfileview反而请求不到资源了
//                        只能在kkfileview的请求参数上做文章

            webView.getEngine().load(tf_kkfileview_url.getText() + api + Base64Utils.encode(url));
        } else {
            // 如果没有节点被选中
            System.out.println("没有选中任何节点");
            showAlert(Alert.AlertType.INFORMATION, "提示", null, "没有选中任何文件，请确定是否加载成功");
        }
    }

    // 获取oss资源的后缀
    public static String getFileExtension(String url) {
        int lastDotIndex = url.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            return url.substring(lastDotIndex).toLowerCase();
        }
        return "";
    }

    public static void showAlert(Alert.AlertType type, String title, String headerText, String contentText) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    //删除url末尾的/
    private String remove(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
