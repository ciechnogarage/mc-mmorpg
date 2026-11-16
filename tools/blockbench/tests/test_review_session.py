import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

class SessionTests(unittest.TestCase):
    def test_bad_exports_are_rejected(self):
        try:
            from review_session import decode_export
        except ImportError:
            self.fail('export validation is missing')
        for response in ({'content': []}, {'isError': True}, {'content': [{'type': 'text', 'text': '{"truncated":true,"content":"{}"}'}]}):
            with self.subTest(response=response), self.assertRaises(ValueError):
                decode_export(response)

if __name__ == '__main__': unittest.main()
