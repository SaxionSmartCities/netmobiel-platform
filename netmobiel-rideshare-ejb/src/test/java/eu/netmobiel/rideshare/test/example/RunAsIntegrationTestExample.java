package eu.netmobiel.rideshare.test.example;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.Callable;

import javax.ejb.EJBAccessException;
import javax.inject.Inject;

import org.junit.Test;


public class RunAsIntegrationTestExample {
    @Inject
    private ExampleService bookshelfService;
    @Inject
    private RunAsManagerExample manager;


    @Test
    public void testAsManager() throws Exception {
        manager.call(new Callable<Example>() {
            @Override
            public Example call() throws Exception {
                bookshelfService.addExample(new Example("978-1-4302-4626-8", "Beginning Java EE 7"));
                bookshelfService.addExample(new Example("978-1-4493-2829-0", "Continuous Enterprise Development in Java"));

                List<Example> books = bookshelfService.getExamples();
                assertEquals("List.size()", 2, books.size());

                for (Example book : books) {
                    bookshelfService.deleteExample(book);
                }

                assertEquals("ExampleService.getExamples()", 0, bookshelfService.getExamples().size());
                return null;
            }
        });
    }

    @Test
    public void testUnauthenticated() throws Exception {
        try {
            bookshelfService.addExample(new Example("978-1-4302-4626-8", "Beginning Java EE 7"));
            fail("Unauthenticated users should not be able to add books");
        } catch (EJBAccessException e) {
            // Good, unauthenticated users cannot add things
        }

        try {
            bookshelfService.deleteExample(null);
            fail("Unauthenticated users should not be allowed to delete");
        } catch (EJBAccessException e) {
            // Good, unauthenticated users cannot delete things
        }

        try {
            // Read access should be allowed
            List<Example> books = bookshelfService.getExamples();
            assertEquals("ExampleService.getExamples()", 0, books.size());
        } catch (EJBAccessException e) {
            fail("Read access should be allowed");
        }
    }
}

