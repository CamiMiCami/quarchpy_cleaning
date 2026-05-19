# Cleaning up the QuarchPy package
Remove unused packages and Java dependency!!!!!!!!!
- Especially QIS, QPS...
- decompiled qis.jar and QuarchCommon.jar from .../quarchpy/connection_specifics/QPS/..., so that I can analyze the algo behind QIS

## So Far:
What can be removed:

------------Folders-----------------
- /quarchpy/fio/: Do we need this?
- /quarchpy/iometer/: Template/ICF generation, execution, result processing
- /quarchpy/disk_test/: Disk detection and target selection
- /quarchpy/docs/: Documentation creation
- /quarchpy/debug/: for emergencies --> maybe needed

------------Imports-----------------
- NumPy: not used in the actual package but may be important when implementing the stream in Python
- pandas: not needed, but one function uses it (needs to be removed)
- 

