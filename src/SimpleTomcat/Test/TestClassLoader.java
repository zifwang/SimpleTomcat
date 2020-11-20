package SimpleTomcat.Test;

public class TestClassLoader {
    public static void main(String[] args) {
        // Object is in BootStrap class loader which is complied by C++
        System.out.println(Object.class.getClassLoader()); // comes with null
        // TestClassLoader is controlled by Application Class Loader
        System.out.println(TestClassLoader.class.getClassLoader()); // String after @ is virtual address
    }
}
