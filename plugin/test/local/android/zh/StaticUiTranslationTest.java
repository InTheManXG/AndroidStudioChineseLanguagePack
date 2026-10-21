package local.android.zh;

import javax.swing.*;

/** 防止补译误改用户输入或表格、列表的渲染数据。 */
public final class StaticUiTranslationTest {
    public static void main(String[] args) throws Exception {
        JLabel dynamicTooltip = new JLabel("保留文字") {
            @Override public String getToolTipText() { throw new AssertionError("不能触发动态提示计算"); }
        };
        StaticUiTranslation.translate(dynamicTooltip);
        SwingUtilities.invokeAndWait(() -> {
            verifyBackgroundHeaders();
            verifyApkHeaders();
            JPanel panel = new JPanel();
            JLabel label = new JLabel("Android SDK Location:");
            JTextField input = new JTextField("Android SDK Location:");
            CellRendererPane renderer = new CellRendererPane();
            JLabel data = new JLabel("Android SDK Location:");
            renderer.add(data);
            JTable sdkTable = new JTable(new Object[][] {{"unchanged", 37, 2, "Installed"}}, new String[] {"Name", "API Level", "Revision", "Status"});
            JPanel tablePanel = new JPanel(); tablePanel.add(sdkTable);
            panel.add(label); panel.add(input); panel.add(renderer); panel.add(tablePanel);
            StaticUiTranslation.translate(panel);
            assert label.getText().equals("Android SDK 位置：") : label.getText();
            assert input.getText().equals("Android SDK Location:") : "误改编辑框";
            assert data.getText().equals("Android SDK Location:") : "误改渲染器数据";
            assert sdkTable.getColumn("Name").getHeaderValue().equals("名称") : "未翻译 SDK 表头";
            assert sdkTable.getModel().getColumnName(0).equals("Name") : "误改模型列名";
            assert sdkTable.getValueAt(0, 3).equals("Installed") : "误改表格数据";
            JComboBox<String> car=new JComboBox<>(new String[]{"Android Automotive OS only","Android Automotive OS and Android Auto"});
            StaticUiTranslation.translate(car);
            var carRenderer=car.getRenderer();
            assert ((JLabel)carRenderer.getListCellRendererComponent(new JList<>(),car.getItemAt(0),0,false,false)).getText().equals("仅 Android Automotive OS");
            assert ((JLabel)carRenderer.getListCellRendererComponent(new JList<>(),car.getItemAt(1),1,false,false)).getText().equals("Android Automotive OS 与 Android Auto");
            assert car.getSelectedItem().equals("Android Automotive OS only");
            car.setSelectedIndex(1);
            assert car.getSelectedItem().equals("Android Automotive OS and Android Auto");
            StaticUiTranslation.translate(car);
            assert car.getRenderer()==carRenderer : "重复包装渲染器";
            JComboBox<String> unrelated=new JComboBox<>(new String[]{"Android Automotive OS only","用户选项"});
            var originalRenderer=unrelated.getRenderer();
            StaticUiTranslation.translate(unrelated);
            assert unrelated.getRenderer()==originalRenderer : "误改相似的用户数据列表";
            StaticUiTranslation.translate(panel);
            assert label.getText().equals("Android SDK 位置：") : "重复处理不幂等";
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                JFrame frame = new JFrame("Device Manager");
                try {
                    StaticUiTranslation.translate(frame);
                    assert frame.getTitle().equals("设备管理器") : "独立窗口标题未补译";
                } finally { frame.dispose(); }
            }
        });
        System.out.println("通过：静态标签补译、输入保护、渲染器保护、重复处理幂等、SDK 表头与数据隔离。");
    }

    private static void verifyBackgroundHeaders() {
        String[] names={"Class","Status","Start","Retries"};
        String[] translated={"类","状态","开始时间","重试次数"};
        JTable table=new JTable(new Object[][]{{"user.MyWorker","ENQUEUED",123L,2}},names);
        try {
            // 使用发行版真实排序器，验证只改表头，不改变原列标识、排序或模型数据。
            var type=Class.forName("com.android.tools.adtui.common.ColumnTreeBuilder$ColumnTreeTableRowSorter");
            var ctor=type.getDeclaredConstructor(javax.swing.table.TableModel.class,java.util.List.class);
            ctor.setAccessible(true);
            @SuppressWarnings("unchecked")
            var sorter=(javax.swing.table.TableRowSorter<javax.swing.table.TableModel>)ctor.newInstance(table.getModel(),
                java.util.List.of(SortOrder.ASCENDING,SortOrder.ASCENDING,SortOrder.ASCENDING,SortOrder.ASCENDING));
            table.setRowSorter(sorter);
            sorter.toggleSortOrder(2);
            var sortKeys=java.util.List.copyOf(sorter.getSortKeys());
            StaticUiTranslation.translate(table);
            StaticUiTranslation.translate(table);
            for(int i=0;i<names.length;i++) {
                assert table.getColumn(names[i]).getHeaderValue().equals(translated[i]):names[i];
                assert table.getModel().getColumnName(i).equals(names[i]);
            }
            assert sorter.getSortKeys().equals(sortKeys);
            assert table.getValueAt(0,0).equals("user.MyWorker") && table.getValueAt(0,1).equals("ENQUEUED");
            assert table.getValueAt(0,2).equals(123L) && table.getValueAt(0,3).equals(2);
            JTable userTable=new JTable(1,4);
            userTable.setModel(new javax.swing.table.DefaultTableModel(names,1));
            StaticUiTranslation.translate(userTable);
            assert userTable.getColumn("Class").getHeaderValue().equals("Class"):"误改同名用户表格";
        } catch(ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static void verifyApkHeaders() {
        String[][] groups={{"File","Size","Download Size","% of Download Size","Compressed","Alignment"},{"File","Size","Download Size","% of Download Size"},{"File","Old Size","New Size","Diff Size"},{"File","Old Size","New Size","Patch Size"}};
        String[][] translated={{"文件","大小","下载大小","下载大小占比","已压缩","对齐方式"},{"文件","大小","下载大小","下载大小占比"},{"文件","旧大小","新大小","大小差异"},{"文件","旧大小","新大小","补丁大小"}};
        try {
            var sorterType=Class.forName("com.android.tools.adtui.common.ColumnTreeBuilder$ColumnTreeTableRowSorter");
            var sorterCtor=sorterType.getDeclaredConstructor(javax.swing.table.TableModel.class,java.util.List.class);sorterCtor.setAccessible(true);
            var builderType=Class.forName("com.android.tools.adtui.common.ColumnTreeBuilder$ColumnBuilder");
            var rendererType=Class.forName("com.android.tools.adtui.common.ColumnTreeBuilder$ColumnBuilder$1");
            var rendererCtor=rendererType.getDeclaredConstructor(builderType,javax.swing.table.TableCellRenderer.class,boolean.class);rendererCtor.setAccessible(true);
            for(int group=0;group<groups.length;group++) {
                String[] names=groups[group];JTable table=new JTable(new Object[][]{{"user.apk",100,80,20}},names);
                @SuppressWarnings("unchecked")
                var sorter=(javax.swing.table.TableRowSorter<javax.swing.table.TableModel>)sorterCtor.newInstance(table.getModel(),java.util.Collections.nCopies(names.length,SortOrder.ASCENDING));
                table.setRowSorter(sorter);sorter.toggleSortOrder(3);var keys=java.util.List.copyOf(sorter.getSortKeys());
                var renderer=(javax.swing.table.TableCellRenderer)rendererCtor.newInstance(builderType.getConstructor().newInstance(),table.getTableHeader().getDefaultRenderer(),true);
                StaticUiTranslation.translate(table);StaticUiTranslation.translate(table);
                for(int i=0;i<names.length;i++) {
                    var column=table.getColumn(names[i]);assert column.getHeaderValue().equals(translated[group][i]);
                    assert table.getModel().getColumnName(i).equals(names[i]);
                    // 执行原版表头renderer，确认文字和tooltip均使用中文headerValue。
                    var label=(JLabel)renderer.getTableCellRendererComponent(table,column.getHeaderValue(),false,false,-1,i);
                    assert label.getText().equals(translated[group][i]) && label.getToolTipText().equals(translated[group][i]);
                }
                assert sorter.getSortKeys().equals(keys) && table.getValueAt(0,0).equals("user.apk") && table.getValueAt(0,3).equals(20);
                JTable other=new JTable(new Object[][]{{"user",1,2,3}},names);other.setAutoCreateRowSorter(true);
                StaticUiTranslation.translate(other);assert other.getColumn("File").getHeaderValue().equals("File");
            }
            System.out.println("通过：四种APK表格（含当前六列表头）真实排序器与表头renderer，中文标题/tooltip、原列名/标识/数据/排序及同名普通表格保护。");
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
    }

}
