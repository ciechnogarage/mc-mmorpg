"""Measure foot movement during ground-contact phases in world space.

Input positions and output displacements use the model's own length units. The
caller must provide ``root_world`` explicitly; this module does not infer a
transport speed. A sampled ``point`` must keep correspondence to the same
material/anatomical sole point across samples. Re-selecting whichever vertex is
lowest as a rotating foot moves can create apparent displacement that this
analyzer cannot distinguish from slip.
"""

from math import hypot, isfinite


FEET = ("right", "left")


def _number(value, label):
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"{label} must be a finite number")
    value = float(value)
    if not isfinite(value):
        raise ValueError(f"{label} must be finite")
    return value


def _vector3(value, label):
    if not isinstance(value, (list, tuple)) or len(value) != 3:
        raise ValueError(f"{label} must contain exactly three finite numbers")
    return [_number(component, f"{label}[{index}]") for index, component in enumerate(value)]


def _normalized_samples(samples):
    try:
        samples = list(samples)
    except TypeError as error:
        raise ValueError("samples must be an iterable of sample mappings") from error

    normalized = []
    previous_time = None
    for index, sample in enumerate(samples):
        if not isinstance(sample, dict):
            raise ValueError(f"samples[{index}] must be a mapping")

        time = _number(sample.get("time"), f"samples[{index}].time")
        if previous_time is not None and time <= previous_time:
            raise ValueError("sample times must be strictly increasing")
        previous_time = time

        root = _vector3(sample.get("root_world"), f"samples[{index}].root_world")
        feet = sample.get("feet")
        if not isinstance(feet, dict):
            raise ValueError(f"samples[{index}].feet must be a mapping")

        normalized_feet = {}
        for foot in FEET:
            foot_sample = feet.get(foot)
            if not isinstance(foot_sample, dict):
                raise ValueError(f"samples[{index}].feet.{foot} must be a mapping")
            normalized_feet[foot] = {
                "min_y": _number(
                    foot_sample.get("min_y"),
                    f"samples[{index}].feet.{foot}.min_y",
                ),
                "point": _vector3(
                    foot_sample.get("point"),
                    f"samples[{index}].feet.{foot}.point",
                ),
            }

        normalized.append({"time": time, "root_world": root, "feet": normalized_feet})
    return normalized


def _new_stance(time, world_point):
    return {
        "start_time": time,
        "end_time": time,
        "sample_count": 1,
        "initial_world_point": list(world_point),
        "final_world_point": list(world_point),
        "max_horizontal_excursion": 0.0,
        "max_horizontal_excursion_time": time,
    }


def analyze_foot_contacts(samples, *, contact_epsilon=0.04, ground_y=0.0):
    """Return world-space horizontal excursion for each contiguous stance.

    A foot is in contact when ``root_world.y + min_y`` is no higher than
    ``ground_y + contact_epsilon``. Only contact samples contribute to
    excursion. Each stance measures horizontal distance from its first sampled
    world-space support point, so swing travel cannot inflate the result.
    """
    contact_epsilon = _number(contact_epsilon, "contact_epsilon")
    if contact_epsilon < 0.0:
        raise ValueError("contact_epsilon must be non-negative")
    ground_y = _number(ground_y, "ground_y")
    samples = _normalized_samples(samples)

    foot_reports = {}
    for foot in FEET:
        stances = []
        current = None
        contact_sample_count = 0

        for sample in samples:
            root = sample["root_world"]
            foot_sample = sample["feet"][foot]
            in_contact = root[1] + foot_sample["min_y"] <= ground_y + contact_epsilon

            if not in_contact:
                if current is not None:
                    stances.append(current)
                    current = None
                continue

            contact_sample_count += 1
            local_point = foot_sample["point"]
            world_point = [root[axis] + local_point[axis] for axis in range(3)]
            if current is None:
                current = _new_stance(sample["time"], world_point)
                continue

            current["end_time"] = sample["time"]
            current["sample_count"] += 1
            current["final_world_point"] = world_point
            initial = current["initial_world_point"]
            excursion = hypot(world_point[0] - initial[0], world_point[2] - initial[2])
            if excursion > current["max_horizontal_excursion"]:
                current["max_horizontal_excursion"] = excursion
                current["max_horizontal_excursion_time"] = sample["time"]

        if current is not None:
            stances.append(current)

        max_excursion = max(
            (stance["max_horizontal_excursion"] for stance in stances),
            default=0.0,
        )
        foot_reports[foot] = {
            "contact_sample_count": contact_sample_count,
            "swing_sample_count": len(samples) - contact_sample_count,
            "stances": stances,
            "max_horizontal_excursion": max_excursion,
        }

    return {
        "units": "model_units",
        "contact_epsilon": contact_epsilon,
        "ground_y": ground_y,
        "sample_count": len(samples),
        "feet": foot_reports,
        "max_horizontal_excursion": max(
            foot_reports[foot]["max_horizontal_excursion"] for foot in FEET
        ),
    }
