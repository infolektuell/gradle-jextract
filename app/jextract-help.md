Usage: jextract <options> <header file>                                                         

Option                             Description                                                  
------                             -----------                                                  
-?, -h, --help                     print help                                                   
-D --define-macro <macro>=<value>  define <macro> to <value> (or 1 if <value> omitted)          
-I, --include-dir <dir>            add directory to the end of the list of include search paths 
--dump-includes <file>             dump included symbols into specified file                    
--header-class-name <name>         name of the generated header class. If this option is not    
                                   specified, then header class name is derived from the header
                                   file name. For example, class "foo_h" for header "foo.h".   
--include-function <name>          name of function to include                                  
--include-constant <name>          name of macro or enum constant to include                    
--include-struct <name>            name of struct definition to include                         
--include-typedef <name>           name of type definition to include                           
--include-union <name>             name of union definition to include                          
--include-var <name>               name of global variable to include                           
-l, --library <libspec>            specify a shared library that should be loaded by the        
                                   generated header class. If <libspec> starts with :, then  
                                   what follows is interpreted as a library path. Otherwise,   
                                   <libspec> denotes a library name. Examples:                 
                                      -l GL                                                    
                                      -l :libGL.so.1                                           
                                      -l :/usr/lib/libGL.so.1                                  
--use-system-load-library          libraries specified using -l are loaded in the loader symbol 
                                   lookup (using either System::loadLibrary, or System::load). 
                                   Useful if the libraries must be loaded from one of the paths
                                   in java.library.path.                                     
--output <path>                    specify the directory to place generated files. If this      
                                   option is not specified, then current directory is used.    
-t, --target-package <package>     target package name for the generated classes. If this option
                                   is not specified, then unnamed package is used.             
--version                          print version information and exit                           
