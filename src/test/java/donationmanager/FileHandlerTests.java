package donationmanager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FileHandlerTests {
    private JFileChooser mockedFileChooser;
    private FileHandler fileHandler;

    @BeforeEach
    public void setUp() throws IllegalAccessException {
        fileHandler = new FileHandler();

        mockedFileChooser = spy(new JFileChooser());

        Field field = ReflectionUtils.findFields(FileHandler.class,
                                                 f -> f.getName().equals("fileChooser"),
                                                 ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);

        field.setAccessible(true);
        field.set(fileHandler, mockedFileChooser);
    }

    @Test
    public void test_choose_file_to_open() {
        // given
        FileFilter filterExpected = FileFilter.PDF;
        String titleExpected = "title";
        File fileExpected = new File("test-files/test.txt");

        doReturn(JFileChooser.APPROVE_OPTION).when(mockedFileChooser).showOpenDialog(null);
        doReturn(fileExpected).when(mockedFileChooser).getSelectedFile();

        // when
        File fileActual = fileHandler.chooseFileToOpen(titleExpected, filterExpected);

        // then
        assertEquals(fileExpected, fileActual);

        verify(mockedFileChooser).setFileFilter(any(FileNameExtensionFilter.class));
        verify(mockedFileChooser).setDialogTitle(titleExpected);

        FileNameExtensionFilter filterActual = (FileNameExtensionFilter) mockedFileChooser.getFileFilter();
        assertEquals(filterExpected.description, filterActual.getDescription());
        assertArrayEquals(filterExpected.extensions, filterActual.getExtensions());

    }

    @Test
    public void test_cancelled_file_dialogue() {
        // given
        doReturn(JFileChooser.CANCEL_OPTION).when(mockedFileChooser).showOpenDialog(null);

        // when then
        Exception exception = assertThrows(RuntimeException.class,
                                           () -> fileHandler.chooseFileToOpen("title", FileFilter.PDF));

        assertEquals("No file chosen", exception.getMessage());
    }
}
