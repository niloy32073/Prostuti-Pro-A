# The base app reaches the engine by reflection (it cannot depend on an on-demand module).
-keep class com.prostuti.pyruntime.PyEngine { public static *; }
